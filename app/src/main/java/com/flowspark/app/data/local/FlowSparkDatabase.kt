package com.flowspark.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flowspark.app.data.local.dao.HistoryDao
import com.flowspark.app.data.local.dao.WorkflowDao
import com.flowspark.app.data.local.entity.HistoryEntity
import com.flowspark.app.data.local.entity.WorkflowEntity
import com.flowspark.app.domain.model.StepConverters

@Database(
    entities = [WorkflowEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StepConverters::class)
abstract class FlowSparkDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "flowspark.db"

        @Volatile
        private var INSTANCE: FlowSparkDatabase? = null

        fun getInstance(context: android.content.Context): FlowSparkDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    FlowSparkDatabase::class.java,
                    NAME,
                ).build().also { INSTANCE = it }
            }
    }
}
