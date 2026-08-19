package com.flowspark.app.ai

import android.graphics.Bitmap
import com.flowspark.app.domain.model.ParsedWorkflow

/**
 * 统一 AI 接口 —— v2.1 核心抽象。
 * 只认 OpenAI-Compatible 协议，不关心背后供应商是谁。
 *
 * 实现类：[OpenAiCompatibleClient]
 */
interface AiClient {

    /**
     * 意图解析：用户口语 → 结构化工作流。
     * 使用 Function Calling 强制输出 JSON。
     */
    suspend fun parseIntent(userInput: String, systemPrompt: String): Result<ParsedWorkflow>

    /**
     * 文生图。
     * @param prompt 图片描述
     * @return 解码后的 Bitmap
     */
    suspend fun generateImage(prompt: String): Result<Bitmap>

    /**
     * 图生图（参考图 + 提示词）。
     * @param prompt 编辑描述
     * @param referenceImage 输入参考图（Base64 编码后发送）
     * @return 解码后的 Bitmap
     */
    suspend fun editImage(prompt: String, referenceImage: Bitmap): Result<Bitmap>
}
