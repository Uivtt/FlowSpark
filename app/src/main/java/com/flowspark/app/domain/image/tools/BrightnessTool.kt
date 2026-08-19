package com.flowspark.app.domain.image.tools

import android.graphics.Bitmap
import android.graphics.Color
import com.flowspark.app.domain.image.ImageTool
import com.flowspark.app.domain.model.StepType

/**
 * 调整亮度。
 * params: brightness（-255 ~ +255，默认 +30；正值变亮，负值变暗）
 */
class BrightnessTool : ImageTool {
    override val supportedType: StepType = StepType.BRIGHTNESS

    override fun apply(input: Bitmap, params: Map<String, String>): Bitmap {
        val delta = (params["brightness"]?.toIntOrNull() ?: 30).coerceIn(-255, 255)
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val width = output.width
        val height = output.height
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (Color.red(c) + delta).coerceIn(0, 255)
            val g = (Color.green(c) + delta).coerceIn(0, 255)
            val b = (Color.blue(c) + delta).coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
