package com.example.forestguardians

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin
import kotlin.random.Random

/**
 * Firefly - Đom đóm & bụi ánh sáng ma thuật bay lơ lửng trong rừng đêm.
 */
class Firefly(val screenWidth: Int, val screenHeight: Int) {
    var x = Random.nextFloat() * screenWidth
    var y = Random.nextFloat() * screenHeight
    var vx = Random.nextFloat() * 1.6f - 0.8f
    var vy = Random.nextFloat() * 1.6f - 0.8f
    var size = Random.nextFloat() * 4f + 2f
    var alphaOffset = Random.nextFloat() * 100f

    fun update(w: Int, h: Int) {
        x += vx
        y += vy
        if (x < 0) x = w.toFloat()
        if (x > w) x = 0f
        if (y < 0) y = h.toFloat()
        if (y > h) y = 0f
    }

    fun draw(canvas: Canvas, paint: Paint) {
        val pulse = (sin(System.currentTimeMillis() * 0.003f + alphaOffset) * 80 + 150).toInt().coerceIn(0, 255)
        paint.color = Color.rgb(180, 255, 210)
        paint.alpha = pulse
        canvas.drawCircle(x, y, size, paint)
        paint.alpha = 255
    }
}
