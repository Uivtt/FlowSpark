package com.flowspark.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.flowspark.app.MainActivity
import com.flowspark.app.R
import com.flowspark.app.ai.AiClient
import com.flowspark.app.ai.OpenAiCompatibleClient
import com.flowspark.app.data.local.FlowSparkDatabase
import com.flowspark.app.data.local.entity.HistoryEntity
import com.flowspark.app.data.prefs.SettingsRepository
import com.flowspark.app.domain.executor.CloudStepHandler
import com.flowspark.app.domain.executor.LinearExecutor
import com.flowspark.app.domain.image.ImageToolRegistry
import com.flowspark.app.domain.model.ExecutionState
import com.flowspark.app.domain.model.Step
import com.flowspark.app.domain.model.StepType
import com.flowspark.app.util.BitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 前台执行服务:保证工作流在后台/锁屏时不被杀死。
 *
 * 通过 Intent Extra 接收:
 *  - [EXTRA_STEPS_JSON]:步骤 JSON 数组
 *  - [EXTRA_INPUT_URI]:输入图片 Uri(可为空,文生图不需要)
 *
 * 每步更新进度通知;结束/失败时:
 *  - 把最终结果位图保存到 cacheDir 并通过 [ACTION_WORKFLOW_RESULT] 广播回 UI;
 *  - 发结果通知。
 */
class FlowSparkExecutionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var executionJob: Job? = null

    private lateinit var database: FlowSparkDatabase
    private lateinit var settings: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        database = FlowSparkDatabase.getInstance(this)
        settings = SettingsRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val stepsJson = intent.getStringExtra(EXTRA_STEPS_JSON) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val inputUriString = intent.getStringExtra(EXTRA_INPUT_URI)

        // 前台通知(立即启动,避免 ANR)
        startForeground(NOTIFICATION_ID, buildProgressNotification("准备中...", 0, 1))

        executionJob = serviceScope.launch {
            try {
                val steps = parseSteps(stepsJson)
                val inputBitmap = inputUriString?.let {
                    BitmapLoader.loadScaled(this@FlowSparkExecutionService, Uri.parse(it))
                }

                // 读取供应商配置,构建 AiClient(配置来自 DataStore)
                val config = settings.aiProvider.first()
                val aiClient: AiClient = OpenAiCompatibleClient(config)

                // 云端步骤处理器:文生图 / 图生图
                val cloudHandler = CloudStepHandler { step, current ->
                    when (step.type) {
                        StepType.TEXT_TO_IMAGE -> {
                            val prompt = step.params["prompt"] ?: ""
                            aiClient.generateImage(prompt).getOrThrow()
                        }
                        StepType.IMAGE_TO_IMAGE -> {
                            requireNotNull(current) { "图生图需要输入图片" }
                            val prompt = step.params["prompt"] ?: ""
                            aiClient.editImage(prompt, current).getOrThrow()
                        }
                        else -> current ?: throw IllegalStateException("非云端步骤")
                    }
                }

                val executor = LinearExecutor(
                    localTools = ImageToolRegistry.default(),
                    cloudHandler = cloudHandler,
                )

                // 收集执行状态 → 更新通知;同时接收每步结果,最终结果用于落盘回显
                var finalResult: Bitmap? = null
                executor.execute(steps, inputBitmap, onResult = { finalResult = it })
                    .collect { state: ExecutionState ->
                        updateProgressNotification(state)
                    }

                // 执行成功:保存结果 + 记录历史 + 广播回 UI
                val resultUri = finalResult?.let { saveResultBitmap(it) }
                database.historyDao().insert(
                    HistoryEntity(
                        summary = steps.firstOrNull()?.description ?: "工作流",
                        stepsJson = stepsJson,
                        succeeded = true,
                    )
                )
                broadcastResult(success = true, uri = resultUri, error = null)
                notifyFinished("工作流执行完成 ✅", null)
            } catch (e: Exception) {
                database.historyDao().insert(
                    HistoryEntity(
                        summary = "执行失败",
                        stepsJson = stepsJson,
                        succeeded = false,
                    )
                )
                broadcastResult(success = false, uri = null, error = e.message)
                notifyFinished("工作流执行失败 ❌", e.message)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        executionJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ========== 结果回传 ==========

    /**
     * 把执行结果广播给本应用内的接收方(如 MainViewModel)。
     * setPackage 限定只有自己应用能收到,防止外部应用伪造结果 / 窃取图片 URI。
     */
    private fun broadcastResult(success: Boolean, uri: Uri?, error: String?) {
        val intent = Intent(ACTION_WORKFLOW_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_RESULT_SUCCESS, success)
            uri?.let { putExtra(EXTRA_RESULT_URI, it.toString()) }
            error?.let { putExtra(EXTRA_RESULT_ERROR, it) }
        }
        sendBroadcast(intent)
    }

    /** 把最终结果位图写入 cacheDir(FileProvider 已配置 cache-path),返回可展示的 content:// URI */
    private fun saveResultBitmap(bitmap: Bitmap): Uri? = try {
        val dir = File(cacheDir, "workflow_results")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "result_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) return null
        }
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    } catch (e: Exception) {
        null
    }

    // ========== 通知 ==========

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "工作流执行",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "显示工作流执行进度" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(text: String, progress: Int, max: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("FlowSpark")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateProgressNotification(state: ExecutionState) {
        val max = state.totalSteps.coerceAtLeast(1)
        val progress = (state.currentStepIndex + 1).coerceIn(0, max)
        val text = if (state.running) {
            "步骤 ${state.currentStepIndex + 1}/$max · ${state.currentStepDescription}"
        } else {
            state.message
        }
        NotificationManagerCompat.from(this).notify(
            NOTIFICATION_ID,
            buildProgressNotification(text, progress, max),
        )
    }

    private fun notifyFinished(title: String, detail: String?) {
        val content = if (detail.isNullOrBlank()) title else "$title\n$detail"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(detail ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(this).notify(FINISH_NOTIFICATION_ID, notification)
    }

    // ========== 工具 ==========

    private fun parseSteps(json: String): List<Step> {
        val array = org.json.JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                add(Step.fromJson(array.getJSONObject(i)))
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "flowspark_execution"
        private const val NOTIFICATION_ID = 1001
        private const val FINISH_NOTIFICATION_ID = 1002

        const val EXTRA_STEPS_JSON = "extra_steps_json"
        const val EXTRA_INPUT_URI = "extra_input_uri"

        /** 工作流执行结果广播(仅本应用可接收) */
        const val ACTION_WORKFLOW_RESULT = "com.flowspark.app.action.WORKFLOW_RESULT"
        const val EXTRA_RESULT_SUCCESS = "extra_result_success"
        const val EXTRA_RESULT_URI = "extra_result_uri"
        const val EXTRA_RESULT_ERROR = "extra_result_error"

        /** 构造启动 Intent(Activity/ViewModel 侧使用) */
        fun startIntent(
            context: Context,
            stepsJson: String,
            inputUri: Uri?,
        ): Intent = Intent(context, FlowSparkExecutionService::class.java).apply {
            putExtra(EXTRA_STEPS_JSON, stepsJson)
            inputUri?.let { putExtra(EXTRA_INPUT_URI, it.toString()) }
        }
    }
}