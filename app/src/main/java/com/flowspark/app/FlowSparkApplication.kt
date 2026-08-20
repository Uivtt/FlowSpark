package com.flowspark.app

import android.app.Application
import com.flowspark.app.data.local.FlowSparkDatabase
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

class FlowSparkApplication : Application() {
    lateinit var database: FlowSparkDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = FlowSparkDatabase.getInstance(this)
        // ACRA 崩溃报告
        initAcra {
            if (BuildConfig.DEBUG) {
                dialog {
                    title = "FlowSpark 崩溃"
                    text = "应用发生了崩溃，请描述您在做什么:"
                    positiveButtonText = "发送"
                    negativeButtonText = "忽略"
                }
            } else {
                httpSender {
                    uri = if (BuildConfig.ACRA_URI.isNotBlank()) {
                        BuildConfig.ACRA_URI
                    } else {
                        "https://flowspark-crash.example.com/report"
                    }
                    format = HttpSender.Type.JSON
                }
            }
        }
    }
}
