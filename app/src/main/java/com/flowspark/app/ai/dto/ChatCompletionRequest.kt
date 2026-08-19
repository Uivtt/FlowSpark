package com.flowspark.app.ai.dto

import com.google.gson.annotations.SerializedName

/** OpenAI-Compatible Chat Completions 请求体 */
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolSpec>? = null,
    @SerializedName("tool_choice") val toolChoice: ToolChoice? = null,
    val temperature: Double? = 0.0,
    @SerializedName("max_tokens") val maxTokens: Int? = 1024,
)

data class ChatMessage(
    val role: String,   // system / user / assistant / tool
    val content: String,
)

data class ToolSpec(
    val type: String = "function",
    val function: FunctionSpec,
)

data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>,
)

data class ToolChoice(
    val type: String = "function",
    val function: FunctionName,
)

data class FunctionName(val name: String)
