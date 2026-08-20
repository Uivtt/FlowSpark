package com.flowspark.app

import android.app.Application
import android.content.Intent
import com.flowspark.app.data.local.FlowSparkDatabase
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class FlowSparkApplication : Application() {
    lateinit var database: FlowSparkDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = FlowSparkDatabase.getInstance(this)
        initAcra(this) {
            // 崩溃报告发送到开发者配置的 endpoint（默认使用 debug 日志）
            // 生产环境：配置 BuildConfig.ACRA_URI
            if (BuildConfig.DEBUG) {
                // Debug 模式：仅弹窗提示，不上报
                dialog {
                    title = "FlowSpark 崩溃"
                    text = "应用发生了崩溃，请描述您在做什么:"
                    positiveButtonText = "发送"
                    negativeButtonText = "忽略"
                }
            } else {
                // Release 模式：HTTP 上报
                httpSender {
                    uri = if (BuildConfig.ACRA_URI.isNotBlank()) {
                        BuildConfig.ACRA_URI
                    } else {
                        "https://flowspark-crash.example.com/report"
                    }
                    format = StringFormat.JSON
                }
            }
        }
    }
}