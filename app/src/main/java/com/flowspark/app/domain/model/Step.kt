package com.flowspark.app.domain.model

import androidx.room.TypeConverter
import org.json.JSONObject

/**
 * 一个流水线步骤。
 *
 * @param id 步骤唯一 ID
 * @param type 步骤类型
 * @param params 参数（本地工具：brightness 等；云端：prompt 等）
 * @param description 给用户看的步骤说明
 */
data class Step(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: StepType,
    val params: Map<String, String> = emptyMap(),
    val description: String = "",
) {
    /** 步骤是否作用于输入图片（本地工具 / 图生图） */
    val needsInputImage: Boolean
        get() = type != StepType.TEXT_TO_IMAGE

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("params", JSONObject(params))
        put("description", description)
    }

    companion object {
        fun fromJson(json: JSONObject): Step = Step(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            type = StepType.fromApiName(json.optString("type", "GRAYSCALE"))
                ?: StepType.GRAYSCALE,
            params = run {
                val p = json.optJSONObject("params") ?: JSONObject()
                p.keys().asSequence().associateWith { p.optString(it) }
            },
            description = json.optString("description", ""),
        )
    }
}

/** Room 类型转换器：Map<String,String> <-> JSON 字符串 */
class StepConverters {
    @TypeConverter
    fun mapToString(map: Map<String, String>): String = JSONObject(map).toString()

    @TypeConverter
    fun stringToMap(value: String): Map<String, String> = runCatching {
        val obj = JSONObject(value)
        obj.keys().asSequence().associateWith { obj.optString(it) }
    }.getOrDefault(emptyMap())
}
