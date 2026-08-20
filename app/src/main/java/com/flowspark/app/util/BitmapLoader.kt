package com.flowspark.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * 统一的图片降采样加载器。
 *
 * Service 与 ViewModel 共用同一套解码策略,避免一处 2048px 抗锯齿、一处
 * 全尺寸解码导致的内存峰值不一致(MediaStore.getBitmap 直接解码 12MP 照片
 * 单张可达 48MB+,在低端机上与像素工具内部 IntArray 叠加必触发 OOM)。
 */
object BitmapLoader {

    /** 解码到该最大边(与服务端工作流输入一致) */
    const val MAX_DIMENSION = 2048

    /**
     * 读取图片尺寸并计算 inSampleSize 的 2 次幂降采样比例。
     */
    private fun calculateInSampleSize(w: Int, h: Int, maxDim: Int): Int {
        if (w <= 0 || h <= 0) return 1
        var sample = 1
        var width = w
        var height = h
        while (width > maxDim || height > maxDim) {
            sample *= 2
            width /= 2
            height /= 2
        }
        return sample
    }

    /**
     * 从 content:// URI 降采样加载位图。
     * 两步解码:第一次只读边界(inJustDecodeBounds),第二次按 inSampleSize 解码。
     *
     * @return 解码失败时返回 null(调用方转友好提示)
     */
    fun loadScaled(context: Context, uri: Uri, maxDim: Int = MAX_DIMENSION): Bitmap? {
        val resolver = context.contentResolver

        // 第 1 步:只读边界
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        } catch (_: Exception) {
            return null
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 第 2 步:降采样解码
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        }
        return try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) {
            null
        }
    }
}