package com.flowspark.app.ai

import com.flowspark.app.domain.model.ParsedWorkflow
import com.flowspark.app.domain.model.Step
import com.flowspark.app.domain.model.StepType

/**
 * 离线后备意图解析器（端侧Agent备胎）。
 *
 * 当网络不可用时，用关键词匹配替代 LLM 解析。
 * 支持 10 个基础指令，准确率目标 > 92%。
 *
 * 使用方式：AiClient 的 parseIntent 在抛出 UnknownHostException 时，
 * 由 IntentParser 降级调用此解析器。
 */
object OfflineFallbackParser {

    private val rules = listOf(
        ParseRule("黑白|灰度|去色|变灰") { listOf(Step(type = StepType.GRAYSCALE, description = "将图片转为黑白")) },
        ParseRule("裁剪|裁切|切掉") { listOf(Step(type = StepType.CROP, params = mapOf("width" to "400", "height" to "400"), description = "居中裁剪为 400x400")) },
        ParseRule("亮|提亮|调亮") { listOf(Step(type = StepType.BRIGHTNESS, params = mapOf("brightness" to "40"), description = "调亮图片")) },
        ParseRule("暗|调暗|变暗") { listOf(Step(type = StepType.BRIGHTNESS, params = mapOf("brightness" to "-40"), description = "调暗图片")) },
        ParseRule("对比|鲜明") { listOf(Step(type = StepType.CONTRAST, params = mapOf("contrast" to "1.8"), description = "增强对比度")) },
        ParseRule("缩放|放大|缩小|尺寸") { listOf(Step(type = StepType.SCALE, params = mapOf("scale" to "0.5"), description = "缩小为 50%")) },
        ParseRule("生成|画.*图|创建.*图片") { listOf(Step(type = StepType.TEXT_TO_IMAGE, params = mapOf("prompt" to "一张美丽的风景画"), description = "生成图片")) },
        ParseRule("模糊|虚化") { listOf(Step(type = StepType.SCALE, params = mapOf("scale" to "0.2"), description = "模糊效果"), Step(type = StepType.SCALE, params = mapOf("scale" to "5.0"), description = "还原尺寸（模糊效果）")) },
        ParseRule("旋转|翻转") { listOf(Step(type = StepType.CROP, params = mapOf("width" to "200", "height" to "200"), description = "裁剪（旋转效果待实现）")) },
        ParseRule("全部|亮一点.*黑白|黑白.*亮") { listOf(Step(type = StepType.BRIGHTNESS, params = mapOf("brightness" to "30"), description = "调亮"), Step(type = StepType.GRAYSCALE, description = "转为黑白")) },
    )

    data class ParseRule(
        val pattern: String,
        val buildSteps: () -> List<Step>,
    )

    /**
     * 尝试用关键词匹配解析用户输入。
     * @return 匹配成功返回 ParsedWorkflow，失败返回 null
     */
    fun tryParse(userInput: String): ParsedWorkflow? {
        val input = userInput.trim().lowercase()

        // 精确匹配优先级最高
        for (rule in rules) {
            if (Regex(rule.pattern).containsMatchIn(input)) {
                val steps = rule.buildSteps()
                return ParsedWorkflow(
                    steps = steps,
                    summary = "离线模式：" + steps.joinToString(" → ") { it.description },
                    rawJson = "",
                )
            }
        }

        return null
    }
}
