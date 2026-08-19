package com.flowspark.app.domain.image

import android.graphics.Bitmap
import com.flowspark.app.domain.image.tools.*
import com.flowspark.app.domain.model.StepType

/**
 * 本地图像处理工具接口。所有工具必须是无状态、纯函数式的：
 * 输入 Bitmap + 参数，输出新 Bitmap（不修改原图）。
 */
interface ImageTool {
    val supportedType: StepType

    fun apply(input: Bitmap, params: Map<String, String>): Bitmap
}

/** 工具注册表：StepType -> ImageTool */
class ImageToolRegistry(private val tools: List<ImageTool>) {
    private val byType: Map<StepType, ImageTool> = tools.associateBy { it.supportedType }

    fun resolve(type: StepType): ImageTool? = byType[type]

    companion object {
        fun default(): ImageToolRegistry = ImageToolRegistry(
            listOf(
                GrayscaleTool(),
                CropTool(),
                BrightnessTool(),
                ContrastTool(),
                ScaleTool(),
            )
        )
    }
}
