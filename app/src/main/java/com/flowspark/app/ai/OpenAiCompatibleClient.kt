package com.flowspark.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.flowspark.app.ai.dto.*
import com.flowspark.app.data.prefs.AiProviderConfig
import com.flowspark.app.domain.model.ParsedWorkflow
import com.flowspark.app.domain.model.Step
import com.flowspark.app.domain.model.StepType
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * OpenAI-Compatible AI 客户端。
 *
 * 与 [AiClient] 接口的唯一实现。通过配置 [baseUrl] 和 [apiKey]，
 * 可无缝切换 DeepSeek / Groq / Together / OpenAI 等供应商。
 *
 * @param config 供应商配置（来自 DataStore 或 BuildConfig 默认值）
 * @param onTokensUsed 可选回调，用于统计 Token 消耗
 */
class OpenAiCompatibleClient(
    private val config: AiProviderConfig,
    private val onTokensUsed: ((prompt: Int, completion: Int) -> Unit)? = null,
) : AiClient {

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer ${config.apiKey}")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    // ========== 意图解析 ==========

    override suspend fun parseIntent(
        userInput: String,
        systemPrompt: String,
    ): Result<ParsedWorkflow> = withContext(Dispatchers.IO) {
        try {
            val request = ChatCompletionRequest(
                model = config.llmModel,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userInput),
                ),
                tools = listOf(
                    ToolSpec(
                        function = FunctionSpec(
                            name = "build_workflow",
                            description = "把用户口语转换成图像处理工作流",
                            parameters = buildWorkflowSchema(),
                        )
                    )
                ),
                toolChoice = ToolChoice(function = FunctionName("build_workflow")),
                temperature = 0.0,
                maxTokens = 1024,
            )

            val body = gson.toJson(request).toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("${config.baseUrl}/v1/chat/completions")
                .post(body)
                .build()

            val response = httpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            // 解析响应
            val apiResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)

            // 记录 Token 用量
            apiResponse.usage?.let { usage ->
                onTokensUsed?.invoke(
                    usage.promptTokens ?: 0,
                    usage.completionTokens ?: 0,
                )
            }

            // 提取 Function Calling 结果
            val toolCall = apiResponse.choices
                ?.firstOrNull()
                ?.message
                ?.toolCalls
                ?.firstOrNull()
                ?.function
                ?: return@withContext Result.failure(
                    IntentParseException("LLM 未返回 Function Calling 结果: ${responseBody.take(200)}")
                )

            val argsJson = JsonParser.parseString(toolCall.arguments).asJsonObject
            val steps = mutableListOf<Step>()

            val stepsArray = argsJson.getAsJsonArray("steps")
            if (stepsArray != null) {
                for (element in stepsArray) {
                    val obj = element.asJsonObject
                    val typeName = obj.get("tool")?.asString ?: continue
                    val type = StepType.fromApiName(typeName)
                    if (type == null) {
                        // 跳过未知工具类型，不中断解析
                        continue
                    }
                    val params = mutableMapOf<String, String>()
                    obj.getAsJsonObject("params")?.let { p ->
                        p.keySet().forEach { key ->
                            params[key] = p.get(key)?.asString ?: ""
                        }
                    }
                    val desc = obj.get("description")?.asString ?: ""
                    steps.add(Step(type = type, params = params, description = desc))
                }
            }

            if (steps.isEmpty()) {
                return@withContext Result.failure(
                    IntentParseException("LLM 返回了空步骤列表")
                )
            }

            val summary = argsJson.get("summary")?.asString ?: "工作流（${steps.size} 步）"
            Result.success(ParsedWorkflow(steps = steps, summary = summary, rawJson = toolCall.arguments ?: ""))
        } catch (e: Exception) {
            if (e is IntentParseException) Result.failure(e)
            else Result.failure(IntentParseException("意图解析网络异常: ${e.message}", e))
        }
    }

    // ========== 文生图 ==========

    override suspend fun generateImage(prompt: String): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val request = ImageGenerationRequest(
                model = config.imageModel,
                prompt = prompt,
                n = 1,
                size = "1024x1024",
                responseFormat = "b64_json",
            )

            val body = gson.toJson(request).toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("${config.baseUrl}/v1/images/generations")
                .post(body)
                .build()

            val response = httpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""
            val imageResponse = gson.fromJson(responseBody, ImageGenerationResponse::class.java)

            val imageData = imageResponse.data?.firstOrNull()
                ?: return@withContext Result.failure(
                    ImageGenerationException("图像生成返回空数据: ${responseBody.take(200)}")
                )

            val bitmap = decodeToBitmap(imageData)
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(ImageGenerationException("图像生成失败: ${e.message}", e))
        }
    }

    // ========== 图生图 ==========

    override suspend fun editImage(prompt: String, referenceImage: Bitmap): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            try {
                // 参考图编码为 Base64
                val byteStream = ByteArrayOutputStream()
                referenceImage.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                val b64Image = Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)

                // 部分供应商（如 Together）使用 images/generations + 参考图参数
                // OpenAI gpt-image-1 使用 image 参数
                val requestJson = gson.toJson(
                    mapOf(
                        "model" to config.imageModel,
                        "prompt" to prompt,
                        "image" to "data:image/png;base64,$b64Image",
                        "n" to 1,
                        "size" to "1024x1024",
                        "response_format" to "b64_json",
                    )
                )

                val body = requestJson.toRequestBody(jsonMediaType)
                val httpRequest = Request.Builder()
                    .url("${config.baseUrl}/v1/images/generations")
                    .post(body)
                    .build()

                val response = httpClient.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                val imageResponse = gson.fromJson(responseBody, ImageGenerationResponse::class.java)

                val imageData = imageResponse.data?.firstOrNull()
                    ?: return@withContext Result.failure(
                        ImageGenerationException("图生图返回空数据: ${responseBody.take(200)}")
                    )

                val bitmap = decodeToBitmap(imageData)
                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(ImageGenerationException("图生图失败: ${e.message}", e))
            }
        }

    // ========== 工具方法 ==========

    private fun decodeToBitmap(data: ImageGenerationResponse.ImageData): Bitmap {
        // 优先 b64_json，其次 url
        data.b64Json?.let { b64 ->
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) return bitmap
        }

        // 如果是 URL，下载并解码（部分供应商返回 url 字段）
        data.url?.let { url ->
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            val bytes = response.body?.bytes() ?: throw ImageGenerationException("无法下载图片: $url")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) return bitmap
        }

        throw ImageGenerationException("无法解码图片数据")
    }

    companion object {
        /** Function Calling JSON Schema — 与计划书 2.4 节一致 */
        fun buildWorkflowSchema(): Map<String, Any> = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "steps" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "tool" to mapOf(
                                "type" to "string",
                                "enum" to StepType.entries.map { it.name.lowercase() },
                            ),
                            "params" to mapOf("type" to "object"),
                            "description" to mapOf("type" to "string"),
                        ),
                        "required" to listOf("tool"),
                    ),
                ),
                "summary" to mapOf(
                    "type" to "string",
                    "description" to "给用户看的一句话摘要",
                ),
            ),
            "required" to listOf("steps", "summary"),
        )
    }
}

// ========== 自定义异常 ==========

class IntentParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class ImageGenerationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
