package com.flowspark.app.ai

import com.flowspark.app.domain.model.ParsedWorkflow

/**
 * 意图解析器。
 * 封装 [AiClient.parseIntent] 的 System Prompt 管理 + 重试逻辑。
 */
class IntentParser(private val aiClient: AiClient) {

    companion object {
        /**
         * 核心 System Prompt。
         * 锁定输出格式为 JSON Function Calling，不能有多余文本。
         */
        const val SYSTEM_PROMPT: String = """你是一个图像处理工作流助手。你的任务是把用户口语翻译成图像处理步骤。

规则：
1. 你必须使用 build_workflow 函数返回结果，不要输出任何其他文字。
2. steps 数组中每个元素必须包含 tool（工具名）和 params（参数对象）。
3. 可用的工具名（全小写）：grayscale, crop, brightness, contrast, scale, text_to_image, image_to_image
4. 如果用户没有明确指定参数，使用合理的默认值。
5. 如果用户说"调亮"或"暗一点"，使用 brightness 工具，参数 brightness 为正值（变亮）或负值（变暗）。
6. 如果用户说"黑白"或"灰度"，使用 grayscale 工具，不需要参数。
7. 如果用户说"裁剪"或"裁切"，使用 crop 工具，参数 width 和 height 为像素值。
8. 如果用户说"生成"或"画"一张图片，使用 text_to_image 工具，参数 prompt 为图片描述。
9. 如果用户说"换背景"或"基于这张图"生成，使用 image_to_image 工具。
10. 每个步骤必须包含 description 字段，用中文简要说明这步做什么。
11. summary 字段用一句话概括整个工作流。"""
    }

    suspend fun parse(userInput: String): Result<ParsedWorkflow> {
        // 首次尝试
        val first = aiClient.parseIntent(userInput, SYSTEM_PROMPT)
        if (first.isSuccess) return first

        val error = first.exceptionOrNull()
        // 只有 JSON 解析类错误才值得重试（网络上/解析器把错误喂回 LLM 没有意义，还浪费 token）
        if (error !is IntentParseException) return first

        val errorMsg = error.message ?: "解析失败"
        val retryPrompt = userInput + "\n\n（注意：上次解析失败: $errorMsg。请确保使用 build_workflow 函数返回合法 JSON。）"
        return aiClient.parseIntent(retryPrompt, SYSTEM_PROMPT)
    }
}
