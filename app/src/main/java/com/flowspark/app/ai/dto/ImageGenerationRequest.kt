package com.flowspark.app.ai.dto

import com.google.gson.annotations.SerializedName

/**
 * OpenAI-Compatible 图像生成请求体。
 * Together AI (Flux) 与 OpenAI Images API 均兼容此格式。
 */
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    @SerializedName("response_format") val responseFormat: String = "b64_json",
)

/** 图像生成响应：data[].b64_json 或 data[].url */
data class ImageGenerationResponse(
    val data: List<ImageData>? = null,
    val error: ApiError? = null,
) {
    data class ImageData(
        @SerializedName("b64_json") val b64Json: String? = null,
        val url: String? = null,
    )
}
