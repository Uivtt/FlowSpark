package com.flowspark.app.domain.model

import android.net.Uri

/**
 * 图像处理/生成结果。
 *
 * @param uri 结果图片的 Uri（本地文件 / 下载缓存）
 * @param source 结果来源（本地算法 / 云端生成）
 * @param bytes 云端 base64 解码后的原始字节（用于落盘）
 */
data class ImageResult(
    val uri: Uri? = null,
    val source: Source = Source.LOCAL,
    val bytes: ByteArray? = null,
) {
    enum class Source { LOCAL, CLOUD }
}
