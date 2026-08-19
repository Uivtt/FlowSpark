package com.flowspark.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flowspark.app.BuildConfig
import com.flowspark.app.data.prefs.AiProviderConfig
import com.flowspark.app.ui.theme.Indigo500

/**
 * 供应商配置界面（v2.1 核心功能）：
 * 修改 baseUrl / apiKey / 模型名即可无缝切换 DeepSeek / Groq / Together / OpenAI。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: AiProviderConfig,
    onSave: (AiProviderConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 编辑态（本地状态，保存时才提交）
    var baseUrl by rememberSaveable { mutableStateOf(currentConfig.baseUrl) }
    var apiKey by rememberSaveable { mutableStateOf(currentConfig.apiKey) }
    var llmModel by rememberSaveable { mutableStateOf(currentConfig.llmModel) }
    var imageModel by rememberSaveable { mutableStateOf(currentConfig.imageModel) }

    // 当外部配置变化时同步到编辑态
    LaunchedEffect(currentConfig) {
        baseUrl = currentConfig.baseUrl
        apiKey = currentConfig.apiKey
        llmModel = currentConfig.llmModel
        imageModel = currentConfig.imageModel
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 说明卡片
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Indigo500.copy(alpha = 0.08f),
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "💡 AI 供应商配置",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FlowSpark 使用 OpenAI 兼容接口，可自由切换供应商：" +
                            "DeepSeek / Groq / Together / OpenAI / 本地 Ollama，零代码改动。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 表单
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("AI 服务地址 (baseUrl)") },
                placeholder = { Text("https://api.deepseek.com") },
                supportingText = {
                    Text("示例：https://api.deepseek.com · https://api.groq.com/openai/v1 · https://api.together.xyz/v1 · https://api.openai.com/v1")
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    Text("建议使用代理层 Key；直连时 App 内请勿硬编码密钥")
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            OutlinedTextField(
                value = llmModel,
                onValueChange = { llmModel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("意图解析模型") },
                placeholder = { Text("deepseek-chat") },
                supportingText = {
                    Text("示例：deepseek-chat · llama-3.3-70b-versatile · gpt-4.1-mini")
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            OutlinedTextField(
                value = imageModel,
                onValueChange = { imageModel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("图像生成模型") },
                placeholder = { Text("black-forest-labs/FLUX.1-schnell") },
                supportingText = {
                    Text("示例：black-forest-labs/FLUX.1-schnell · gpt-image-1")
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            // 恢复默认
            OutlinedButton(
                onClick = {
                    baseUrl = BuildConfig.AI_PROXY_BASE_URL
                    apiKey = BuildConfig.AI_PROXY_API_KEY
                    llmModel = BuildConfig.DEFAULT_LLM_MODEL
                    imageModel = BuildConfig.DEFAULT_IMAGE_MODEL
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("恢复默认")
            }

            // 保存
            Button(
                onClick = {
                    onSave(
                        AiProviderConfig(
                            baseUrl = baseUrl.trim().ifEmpty { BuildConfig.AI_PROXY_BASE_URL },
                            apiKey = apiKey.trim(),
                            llmModel = llmModel.trim().ifEmpty { BuildConfig.DEFAULT_LLM_MODEL },
                            imageModel = imageModel.trim().ifEmpty { BuildConfig.DEFAULT_IMAGE_MODEL },
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
            ) {
                Text("保存并生效")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
