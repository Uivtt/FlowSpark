package com.flowspark.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 执行历史记录。
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val summary: String,
    val stepsJson: String,
    val succeeded: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)
