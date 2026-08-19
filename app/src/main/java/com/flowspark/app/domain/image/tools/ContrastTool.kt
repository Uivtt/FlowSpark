package com.flowspark.app.domain.image.tools

import android.graphics.Bitmap
import android.graphics.Color
import com.flowspark.app.domain.image.ImageTool
import com.flowspark.app.domain.model.StepType

/**
 * 调整对比度（乘性增益，围绕 128 中点）。
 * params: contrast（0.0 ~ 3.0，默认 1.5；1.0 为原图）
 */
class ContrastTool : ImageTool {
    override val supportedType: StepType = StepType.CONTRAST

    override fun apply(input: Bitmap, params: Map<String, String>): Bitmap {
        val factor = (params["contrast"]?.toFloatOrNull() ?: 1.5f).coerceIn(0.0f, 3.0f)
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val width = output.width
        val height = output.height
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = ((Color.red(c) - 128) * factor + 128).toInt().coerceIn(0, 255)
            val g = ((Color.green(c) - 128) * factor + 128).toInt().coerceIn(0, 255)
            val b = ((Color.blue(c) - 128) * factor + 128).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
