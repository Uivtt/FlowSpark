package com.flowspark.app.domain.model

/**
 * 工作流步骤类型。
 * 本地工具步骤 + 云端 AI 步骤。
 */
enum class StepType {
    GRAYSCALE,          // 灰度
    CROP,               // 裁剪
    BRIGHTNESS,         // 亮度
    CONTRAST,           // 对比度
    SCALE,              // 缩放
    TEXT_TO_IMAGE,      // 文生图 (云端)
    IMAGE_TO_IMAGE;     // 图生图 (云端)

    val isCloud: Boolean
        get() = this == TEXT_TO_IMAGE || this == IMAGE_TO_IMAGE

    companion object {
        /** 兼容 LLM 返回的字符串（大小写不敏感，容忍下划线/空格） */
        fun fromApiName(name: String): StepType? =
            entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                    it.name.replace("_", "").equals(name.replace("_", ""), ignoreCase = true)
            }
    }
}
