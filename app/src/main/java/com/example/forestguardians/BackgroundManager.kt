package com.example.forestguardians

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log

/**
 * BackgroundManager - Quản lý hiệu ứng cuộn nền vô tận (Infinite Parallax Scrolling).
 */
class BackgroundManager(context: Context, val screenWidth: Int, val screenHeight: Int) {

    private var bgBitmap: Bitmap? = null
    private var bgX1 = 0f
    private var bgX2 = 0f
    private val scrollSpeed = 3.2f // Tốc độ cuộn nền sang trái
    private var scaledBgWidth = screenWidth

    init {
        try {
            val original = BitmapFactory.decodeResource(context.resources, R.drawable.bg_forest)
            if (original != null) {
                // Tỉ lệ scale sao cho chiều cao ảnh vừa khít màn hình
                val ratio = screenHeight.toFloat() / original.height.toFloat()
                scaledBgWidth = (original.width * ratio).toInt()
                if (scaledBgWidth < screenWidth) scaledBgWidth = screenWidth

                bgBitmap = Bitmap.createScaledBitmap(original, scaledBgWidth, screenHeight, true)
                bgX1 = 0f
                bgX2 = scaledBgWidth.toFloat()
            }
        } catch (e: Exception) {
            Log.e("BackgroundManager", "Không thể nạp tài nguyên bg_forest", e)
        }
    }

    fun update() {
        bgX1 -= scrollSpeed
        bgX2 -= scrollSpeed

        // Khi ảnh 1 cuộn hết về bên trái thì đưa ra sau ảnh 2
        if (bgX1 + scaledBgWidth <= 0) {
            bgX1 = bgX2 + scaledBgWidth
        }
        // Khi ảnh 2 cuộn hết về bên trái thì đưa ra sau ảnh 1
        if (bgX2 + scaledBgWidth <= 0) {
            bgX2 = bgX1 + scaledBgWidth
        }
    }

    fun draw(canvas: Canvas) {
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap!!, bgX1, 0f, null)
            canvas.drawBitmap(bgBitmap!!, bgX2, 0f, null)
        } else {
            canvas.drawColor(Color.rgb(27, 38, 29))
        }
    }
}
