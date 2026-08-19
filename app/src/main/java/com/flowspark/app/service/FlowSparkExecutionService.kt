package com.flowspark.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flowspark.app.BuildConfig
import com.flowspark.app.MainActivity
import com.flowspark.app.R
import com.flowspark.app.ai.AiClient
import com.flowspark.app.ai.IntentParser
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
 * 前台执行服务：保证工作流在后台/锁屏时不被杀死。
 *
 * 通过 Intent Extra 接收：
 *  - [EXTRA_STEPS_JSON]：步骤 JSON 数组
 *  - [EXTRA_INPUT_URI]：输入图片 Uri（可为空，文生图不需要）
 *
 * 每步更新进度通知；结束/失败时发结果通知。
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

        // 前台通知（立即启动，避免 ANR）
        startForeground(NOTIFICATION_ID, buildProgressNotification("准备中…", 0, 1))

        executionJob = serviceScope.launch {
            try {
                val steps = parseSteps(stepsJson)
                val inputBitmap = inputUriString?.let { loadBitmap(Uri.parse(it)) }

                // 读取供应商配置，构建 AiClient（配置来自 DataStore）
                val config = settings.aiProvider.first()
                val aiClient: AiClient = OpenAiCompatibleClient(config)
                val parser = IntentParser(aiClient)

                // 云端步骤处理器：文生图 / 图生图
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

                // 收集执行状态 → 更新通知
                executor.execute(steps, inputBitmap).collect { state: ExecutionState ->
                    updateProgressNotification(state)
                }

                // 执行成功：记录历史
                database.historyDao().insert(
                    HistoryEntity(
                        summary = steps.firstOrNull()?.description ?: "工作流",
                        stepsJson = stepsJson,
                        succeeded = true,
                    )
                )
                notifyFinished("工作流执行完成 ✅", null)
            } catch (e: Exception) {
                database.historyDao().insert(
                    HistoryEntity(
                        summary = "执行失败",
                        stepsJson = stepsJson,
                        succeeded = false,
                    )
                )
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

    private fun loadBitmap(uri: Uri): Bitmap? {
        val resolver = contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options()
        opts.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 2048)
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun calculateInSampleSize(w: Int, h: Int, maxDim: Int): Int {
        var sample = 1
        var width = w
        var height = h
        while (width > maxDim || height > maxDim) {
            sample *= 2
            width /= 2
            height /= 2
        }
        return sample
    }

    companion object {
        private const val CHANNEL_ID = "flowspark_execution"
        private const val NOTIFICATION_ID = 1001
        private const val FINISH_NOTIFICATION_ID = 1002

        const val EXTRA_STEPS_JSON = "extra_steps_json"
        const val EXTRA_INPUT_URI = "extra_input_uri"

        /** 构造启动 Intent（Activity/ViewModel 侧使用） */
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
