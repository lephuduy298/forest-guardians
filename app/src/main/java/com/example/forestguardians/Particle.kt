package com.example.forestguardians

import android.graphics.Canvas
import android.graphics.Paint

/**
 * Particle - Đại diện cho các hạt ánh sáng (khói phản lực, bụi ma thuật, vệt đạn, vụ nổ).
 */
class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Int,
    var size: Float,
    var lifeSpan: Int
) {
    private val maxLife = lifeSpan

    fun update() {
        x += vx
        y += vy
        lifeSpan--
        size = (size * 0.95f).coerceAtLeast(1f)
    }

    fun isDead(): Boolean = lifeSpan <= 0

    fun draw(canvas: Canvas, paint: Paint) {
        val alpha = ((lifeSpan.toFloat() / maxLife) * 255).toInt().coerceIn(0, 255)
        paint.color = color
        paint.alpha = alpha
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, size, paint)
        paint.alpha = 255 // Reset alpha cho lần vẽ kế tiếp
    }
}
