package com.flowspark.app.ai.dto

import com.google.gson.annotations.SerializedName

/** OpenAI-Compatible Chat Completions 响应体（只解析我们关心的字段） */
data class ChatCompletionResponse(
    val choices: List<Choice>? = null,
    val error: ApiError? = null,
    val usage: Usage? = null,
) {
    data class Choice(
        val message: ResponseMessage? = null,
    )

    data class ResponseMessage(
        val role: String? = null,
        val content: String? = null,
        @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    )

    data class ToolCall(
        val id: String? = null,
        val type: String? = null,
        val function: FunctionCall? = null,
    )

    data class FunctionCall(
        val name: String? = null,
        val arguments: String? = null,
    )

    data class Usage(
        @SerializedName("prompt_tokens") val promptTokens: Int? = null,
        @SerializedName("completion_tokens") val completionTokens: Int? = null,
    )
}

/** OpenAI 标准错误格式 */
data class ApiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
