package com.flowspark.app

import android.app.Application
import com.flowspark.app.data.local.FlowSparkDatabase

class FlowSparkApplication : Application() {
    lateinit var database: FlowSparkDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = FlowSparkDatabase.getInstance(this)
    }
}
