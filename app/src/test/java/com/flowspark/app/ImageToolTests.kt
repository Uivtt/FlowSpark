package com.flowspark.app

import android.graphics.Bitmap
import com.flowspark.app.domain.image.ImageToolRegistry
import com.flowspark.app.domain.image.tools.*
import com.flowspark.app.domain.model.StepType
import org.junit.Assert.*
import org.junit.Test

/**
 * 本地图像工具单元测试。
 * 验证 5 个基础工具的正确性。
 */
class ImageToolTests {

    private fun createTestBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            // 渐变：左上角亮蓝，右下角暗红
            android.graphics.Color.rgb(
                (x * 255 / width) % 256,
                (y * 255 / height) % 256,
                ((x + y) * 128 / (width + height)) % 256,
            )
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    @Test
    fun grayscale_maintains_dimensions() {
        val input = createTestBitmap(50, 30)
        val output = GrayscaleTool().apply(input, emptyMap())
        assertEquals(input.width, output.width)
        assertEquals(input.height, output.height)
        assertEquals(Bitmap.Config.ARGB_8888, output.config)
    }

    @Test
    fun grayscale_is_actually_gray() {
        val input = createTestBitmap()
        val output = GrayscaleTool().apply(input, emptyMap())
        val pixels = IntArray(output.width * output.height)
        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        // 检查第一个像素 R=G=B
        val c = pixels[0]
        val r = android.graphics.Color.red(c)
        val g = android.graphics.Color.green(c)
        val b = android.graphics.Color.blue(c)
        assertEquals(r, g, 2)  // 允许 2 的误差（浮点取整）
        assertEquals(g, b, 2)
    }

    @Test
    fun crop_center_twice() {
        val input = createTestBitmap(200, 100)
        val output = CropTool().apply(input, mapOf("width" to "100", "height" to "50"))
        assertEquals(100, output.width)
        assertEquals(50, output.height)
    }

    @Test
    fun crop_clamps_to_input() {
        val input = createTestBitmap(50, 50)
        val output = CropTool().apply(input, mapOf("width" to "200", "height" to "200"))
        // 不能超出输入尺寸
        assertEquals(50, output.width)
        assertEquals(50, output.height)
    }

    @Test
    fun brightness_positive_lighter() {
        val input = createTestBitmap()
        val output = BrightnessTool().apply(input, mapOf("brightness" to "50"))
        val inPixels = IntArray(input.width * input.height)
        val outPixels = IntArray(output.width * output.height)
        input.getPixels(inPixels, 0, input.width, 0, 0, input.width, input.height)
        output.getPixels(outPixels, 0, output.width, 0, 0, output.width, output.height)

        var totalR = 0
        for (i in outPixels.indices) {
            totalR += android.graphics.Color.red(outPixels[i]) - android.graphics.Color.red(inPixels[i])
        }
        val avgDelta = totalR.toFloat() / outPixels.size
        assertTrue("亮度平均增加应接近 50，实际 $avgDelta", avgDelta > 40 && avgDelta < 60)
    }

    @Test
    fun brightness_negative_darker() {
        val input = createTestBitmap()
        val output = BrightnessTool().apply(input, mapOf("brightness" to "-50"))
        val inPixels = IntArray(input.width * input.height)
        val outPixels = IntArray(output.width * output.height)
        input.getPixels(inPixels, 0, input.width, 0, 0, input.width, input.height)
        output.getPixels(outPixels, 0, output.width, 0, 0, output.width, output.height)

        var totalR = 0
        for (i in outPixels.indices) {
            totalR += android.graphics.Color.red(inPixels[i]) - android.graphics.Color.red(outPixels[i])
        }
        val avgDelta = totalR.toFloat() / outPixels.size
        assertTrue("亮度平均减少应接近 50，实际 $avgDelta", avgDelta > 40 && avgDelta < 60)
    }

    @Test
    fun contrast_1_returns_identity() {
        val input = createTestBitmap()
        val output = ContrastTool().apply(input, mapOf("contrast" to "1.0"))
        val inPixels = IntArray(input.width * input.height)
        val outPixels = IntArray(output.width * output.height)
        input.getPixels(inPixels, 0, input.width, 0, 0, input.width, input.height)
        output.getPixels(outPixels, 0, output.width, 0, 0, output.width, output.height)
        assertArrayEquals(inPixels, outPixels)
    }

    @Test
    fun scale_by_factor() {
        val input = createTestBitmap(200, 100)
        val output = ScaleTool().apply(input, mapOf("scale" to "0.5"))
        assertEquals(100, output.width)
        assertEquals(50, output.height)
    }

    @Test
    fun scale_with_explicit_width() {
        val input = createTestBitmap(200, 100)
        val output = ScaleTool().apply(input, mapOf("width" to "50"))
        assertEquals(50, output.width)
        // 保持比例: 200/100 = 50/25
        assertEquals(25, output.height)
    }

    @Test
    fun registry_resolves_all_tools() {
        val registry = ImageToolRegistry.default()
        val types = listOf(
            StepType.GRAYSCALE,
            StepType.CROP,
            StepType.BRIGHTNESS,
            StepType.CONTRAST,
            StepType.SCALE,
        )
        for (type in types) {
            assertNotNull("$type 应该被注册", registry.resolve(type))
        }
        assertNull("TEXT_TO_IMAGE 不应在本地工具中", registry.resolve(StepType.TEXT_TO_IMAGE))
    }
}
