package com.flowspark.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 已保存的工作流配置（步骤序列序列化为 JSON）。
 */
@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** List<Step> 的 JSON 数组字符串 */
    val stepsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)
