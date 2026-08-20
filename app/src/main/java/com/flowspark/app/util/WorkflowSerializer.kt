package com.flowspark.app.util

import com.flowspark.app.domain.model.Step
import com.flowspark.app.domain.model.StepType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 工作流序列化/反序列化工具。
 * 用于导入/导出 .flow.json 文件（兼容计划书标准格式）。
 */
object WorkflowSerializer {

    private const val SCHEMA_VERSION = 1

    data class FlowDocument(
        val version: Int = SCHEMA_VERSION,
        val name: String,
        val steps: List<Step>,
        val createdAt: Long = System.currentTimeMillis(),
    )

    /**
     * 将工作流序列化为 JSON 字符串。
     */
    fun serialize(name: String, steps: List<Step>): String {
        val doc = JSONObject().apply {
            put("version", SCHEMA_VERSION)
            put("name", name)
            put("createdAt", System.currentTimeMillis())
            put("steps", JSONArray(steps.map { it.toJson() }))
        }
        return doc.toString(2)
    }

    /**
     * 从 JSON 字符串反序列化工作流。
     */
    fun deserialize(json: String): Result<FlowDocument> = runCatching {
        val doc = JSONObject(json)
        val version = doc.optInt("version", 1)
        val name = doc.optString("name", "未命名工作流")
        val createdAt = doc.optLong("createdAt", System.currentTimeMillis())
        val stepsArray = doc.getJSONArray("steps")
        val steps = mutableListOf<Step>()
        for (i in 0 until stepsArray.length()) {
            steps.add(Step.fromJson(stepsArray.getJSONObject(i)))
        }
        FlowDocument(version, name, steps, createdAt)
    }
}
