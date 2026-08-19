package com.flowspark.app.domain.image.tools

import android.graphics.Bitmap
import com.flowspark.app.domain.image.ImageTool
import com.flowspark.app.domain.model.StepType

/**
 * 缩放（双线性滤波）。
 * params: scale（倍率，默认 0.5；1.0 原尺寸，2.0 放大两倍）
 * 或 width / height 直接指定输出尺寸。
 */
class ScaleTool : ImageTool {
    override val supportedType: StepType = StepType.SCALE

    override fun apply(input: Bitmap, params: Map<String, String>): Bitmap {
        val scale = params["scale"]?.toFloatOrNull() ?: 0.5f
        val outW: Int
        val outH: Int

        val explicitW = params["width"]?.toIntOrNull()
        val explicitH = params["height"]?.toIntOrNull()
        if (explicitW != null || explicitH != null) {
            val ratio = input.width.toFloat() / input.height
            outW = explicitW ?: (explicitH!! * ratio).toInt()
            outH = explicitH ?: (explicitW!! / ratio).toInt()
        } else {
            outW = (input.width * scale).toInt().coerceAtLeast(1)
            outH = (input.height * scale).toInt().coerceAtLeast(1)
        }

        return Bitmap.createScaledBitmap(input, outW, outH, true)
    }
}
