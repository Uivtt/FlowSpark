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
        val explicitW = params["width"]?.toIntOrNull()
        val explicitH = params["height"]?.toIntOrNull()

        val ratio = input.width.toFloat() / input.height

        // 只给一个维度时,另一个维度按原图宽高比推算(对齐 KDoc 注释)
        // 使用 Pair 解构确保编译器能验证所有路径都初始化了 finalW/finalH
        val (finalW, finalH) = when {
            explicitW != null && explicitH != null -> explicitW to explicitH
            explicitW != null -> explicitW to (explicitW / ratio).toInt().coerceAtLeast(1)
            explicitH != null -> (explicitH * ratio).toInt().coerceAtLeast(1) to explicitH
            else -> input.width / 2 to input.height / 2
        }

        val safeW = finalW.coerceIn(1, input.width)
        val safeH = finalH.coerceIn(1, input.height)

        val x = (input.width - safeW) / 2
        val y = (input.height - safeH) / 2

        return Bitmap.createBitmap(input, x, y, safeW, safeH)
    }
}
