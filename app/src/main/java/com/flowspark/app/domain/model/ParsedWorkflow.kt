package com.flowspark.app.domain.model

/**
 * LLM 意图解析结果 —— 待用户确认的"蓝图"。
 */
data class ParsedWorkflow(
    val steps: List<Step>,
    val summary: String,
    val rawJson: String = "",
)
