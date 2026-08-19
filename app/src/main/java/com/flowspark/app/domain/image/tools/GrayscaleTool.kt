package com.flowspark.app.domain.image.tools

import android.graphics.Bitmap
import android.graphics.Color
import com.flowspark.app.domain.image.ImageTool
import com.flowspark.app.domain.model.StepType

/** 灰度化：BT.601 亮度系数（与安卓 ColorMatrix 标准一致） */
class GrayscaleTool : ImageTool {
    override val supportedType: StepType = StepType.GRAYSCALE

    override fun apply(input: Bitmap, params: Map<String, String>): Bitmap {
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val width = output.width
        val height = output.height
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(gray, gray, gray)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
