package com.flowspark.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowspark.app.BuildConfig
import com.flowspark.app.ai.IntentParser
import com.flowspark.app.ai.OpenAiCompatibleClient
import com.flowspark.app.data.prefs.AiProviderConfig
import com.flowspark.app.data.prefs.SettingsRepository
import com.flowspark.app.domain.executor.CloudStepHandler
import com.flowspark.app.domain.executor.LinearExecutor
import com.flowspark.app.domain.image.ImageToolRegistry
import com.flowspark.app.domain.model.ExecutionState
import com.flowspark.app.domain.model.ImageResult
import com.flowspark.app.domain.model.ParsedWorkflow
import com.flowspark.app.domain.model.Step
import com.flowspark.app.domain.model.StepType
import com.flowspark.app.service.FlowSparkExecutionService
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主屏幕 ViewModel —— 管理全部对话 + 工作流状态。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)
    private val localTools = ImageToolRegistry.default()

    // ========== 状态 ==========

    /** 对话消息列表 */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** 当前输入文字 */
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    /** 正在等待 LLM 回复 */
    private val _parsing = MutableStateFlow(false)
    val parsing: StateFlow<Boolean> = _parsing.asStateFlow()

    /** LLM 解析出的待确认工作流 */
    private val _pendingWorkflow = MutableStateFlow<ParsedWorkflow?>(null)
    val pendingWorkflow: StateFlow<ParsedWorkflow?> = _pendingWorkflow.asStateFlow()

    /** 执行状态 */
    private val _executionState = MutableStateFlow(ExecutionState())
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    /** 当前预览图片 Uri */
    private val _previewUri = MutableStateFlow<Uri?>(null)
    val previewUri: StateFlow<Uri?> = _previewUri.asStateFlow()

    /** 输入图片 Uri */
    private val _inputImageUri = MutableStateFlow<Uri?>(null)
    val inputImageUri: StateFlow<Uri?> = _inputImageUri.asStateFlow()

    /** 是否显示配置界面 */
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    /** 当前 AI 供应商配置（可观察，供配置界面回显） */
    private val _aiProviderConfig = MutableStateFlow(
        AiProviderConfig(
            baseUrl = BuildConfig.AI_PROXY_BASE_URL,
            apiKey = BuildConfig.AI_PROXY_API_KEY,
            llmModel = BuildConfig.DEFAULT_LLM_MODEL,
            imageModel = BuildConfig.DEFAULT_IMAGE_MODEL,
        )
    )
    val aiProviderConfig: StateFlow<AiProviderConfig> = _aiProviderConfig.asStateFlow()

    // ========== 对话操作 ==========

    fun updateInput(text: String) { _inputText.value = text }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _parsing.value) return

        // 添加用户消息
        _messages.value = _messages.value + ChatMessage.user(text)
        _inputText.value = ""
        _parsing.value = true

        viewModelScope.launch {
            try {
                // 读取配置，构建 AiClient
                val config = settings.aiProvider.first()
                val aiClient = OpenAiCompatibleClient(config)
                val parser = IntentParser(aiClient)

                val result = parser.parse(text)
                result.onSuccess { workflow ->
                    _pendingWorkflow.value = workflow
                    _messages.value = _messages.value + ChatMessage.assistant(
                        workflow.summary,
                        workflow,
                    )
                }.onFailure { error ->
                    _messages.value = _messages.value + ChatMessage.assistant(
                        error.message ?: "我没听懂，请换一种说法",
                    )
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage.assistant(
                    "网络开小差了，请稍后重试",
                )
            } finally {
                _parsing.value = false
            }
        }
    }

    // ========== 工作流执行 ==========

    /** 确认执行待处理工作流 */
    fun confirmExecution() {
        val workflow = _pendingWorkflow.value ?: return
        _pendingWorkflow.value = null

        // 如果包含云端步骤，启动 Foreground Service
        if (workflow.steps.any { it.type.isCloud }) {
            val inputUri = _inputImageUri.value
            val context = getApplication<com.flowspark.app.FlowSparkApplication>()
            val intent = FlowSparkExecutionService.startIntent(
                context,
                Gson().toJson(workflow.steps.map { it.toJson() }),
                inputUri,
            )
            context.startForegroundService(intent)
            _messages.value = _messages.value + ChatMessage.system("⏳ 正在后台执行…")
        } else {
            // 纯本地步骤，直接在当前协程执行
            executeLocal(workflow.steps)
        }
    }

    /** 取消待处理工作流 */
    fun cancelWorkflow() {
        _pendingWorkflow.value = null
    }

    private fun executeLocal(steps: List<Step>) {
        viewModelScope.launch {
            _executionState.value = ExecutionState(running = true, totalSteps = steps.size)

            val inputUri = _inputImageUri.value
            var inputBitmap: Bitmap? = null
            if (inputUri != null) {
                val context = getApplication<com.flowspark.app.FlowSparkApplication>()
                inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, inputUri)
            }

            try {
                val executor = LinearExecutor(localTools = localTools, cloudHandler = null)
                executor.execute(steps, inputBitmap).collect { state ->
                    _executionState.value = state
                }
                _messages.value = _messages.value + ChatMessage.system("✅ 本地步骤执行完成")
            } catch (e: Exception) {
                _executionState.value = _executionState.value.copy(
                    running = false,
                    error = e.message,
                )
                _messages.value = _messages.value + ChatMessage.system("❌ 执行失败: ${e.message}")
            }
        }
    }

    // ========== 图片操作 ==========

    fun setInputImage(uri: Uri) {
        _inputImageUri.value = uri
        _previewUri.value = uri
    }

    fun clearInputImage() {
        _inputImageUri.value = null
        _previewUri.value = null
    }

    // ========== 供应商配置 ==========

    /** 初始化时订阅 DataStore 中的配置 */
    init {
        viewModelScope.launch {
            settings.aiProvider.collect { config ->
                _aiProviderConfig.value = config
            }
        }
    }

    fun openSettings() { _showSettings.value = true }

    fun closeSettings() { _showSettings.value = false }

    fun updateProviderConfig(config: AiProviderConfig) {
        viewModelScope.launch {
            settings.updateProvider(config)
            _aiProviderConfig.value = config
        }
    }
}

// ========== 对话消息模型 ==========

data class ChatMessage(
    val role: Role,
    val text: String,
    val workflow: ParsedWorkflow? = null,
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
    companion object {
        fun user(text: String) = ChatMessage(Role.USER, text)
        fun assistant(text: String, workflow: ParsedWorkflow? = null) =
            ChatMessage(Role.ASSISTANT, text, workflow)
        fun system(text: String) = ChatMessage(Role.SYSTEM, text)
    }
}
