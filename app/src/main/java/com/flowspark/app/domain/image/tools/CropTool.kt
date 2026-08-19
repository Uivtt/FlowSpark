package com.flowspark.app.domain.image.tools

import android.graphics.Bitmap
import com.flowspark.app.domain.image.ImageTool
import com.flowspark.app.domain.model.StepType

/**
 * 居中裁剪。
 * params: width（输出宽 px，默认原宽的一半）
 *         height（输出高 px，默认原高的一半）
 *         若只给一个维度，按原图宽高比缩放另一维。
 */
class CropTool : ImageTool {
    override val supportedType: StepType = StepType.CROP

    override fun apply(input: Bitmap, params: Map<String, String>): Bitmap {
        val targetW = params["width"]?.toIntOrNull() ?: (input.width / 2)
        val targetH = params["height"]?.toIntOrNull() ?: (input.height / 2)

        val safeW = targetW.coerceIn(1, input.width)
        val safeH = targetH.coerceIn(1, input.height)

        val x = (input.width - safeW) / 2
        val y = (input.height - safeH) / 2

        return Bitmap.createBitmap(input, x, y, safeW, safeH)
    }
}
