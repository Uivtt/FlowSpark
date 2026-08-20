package com.flowspark.app.util

import com.flowspark.app.domain.model.StepType

/**
 * 智能成本预估工具。
 * 根据步骤类型和供应商单价估算每次执行的费用。
 */
object CostEstimator {

    data class CostBreakdown(
        val totalUsd: Double,
        val details: List<StepCost>,
        val currency: String = "USD",
    ) {
        val formatted: String get() = "$${String.format("%.6f", totalUsd)}"
        val formattedShort: String get() = if (totalUsd < 0.001) "< $0.001" else "$${String.format("%.4f", totalUsd)}"
    }

    data class StepCost(
        val stepDescription: String,
        val type: StepType,
        val costUsd: Double,
    )

    /**
     * 默认单价（按 DeepSeek + Together Flux 廉价路线估算）。
     * 用户可自行在配置中覆盖。
     *
     * LLM 意图解析: ~$0.00005/次 (DeepSeek v3)
     * 图像生成: ~$0.002/张 (Flux.1 低配)
     * 本地工具: $0
     */
    private const val LLM_COST_PER_CALL = 0.00005
    private const val IMAGE_COST_PER_CALL = 0.002

    fun estimate(steps: List<com.flowspark.app.domain.model.Step>): CostBreakdown {
        val details = steps.map { step ->
            val cost = when (step.type) {
                StepType.TEXT_TO_IMAGE, StepType.IMAGE_TO_IMAGE -> IMAGE_COST_PER_CALL
                else -> 0.0
            }
            StepCost(
                stepDescription = step.description.ifBlank { step.type.name },
                type = step.type,
                costUsd = cost,
            )
        }
        // 加上 LLM 解析费用
        val total = details.sumOf { it.costUsd } + LLM_COST_PER_CALL
        return CostBreakdown(totalUsd = total, details = details)
    }
}
