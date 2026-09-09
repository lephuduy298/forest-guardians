package com.example.forestguardians

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/**
 * Bullet - Đối tượng C: Quả cầu năng lượng xoáy sáng bay về phía bên phải.
 */
class Bullet(
    context: Context,
    var x: Float,
    var y: Float,
    val size: Float = 55f
) {
    val speed = 25f // Bay nhanh hơn Robot B rõ rệt
    private var sprite: Bitmap? = null

    init {
        try {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.energy_bullet)
            if (raw != null) {
                sprite = Bitmap.createScaledBitmap(raw, size.toInt(), size.toInt(), true)
            }
        } catch (e: Exception) {
            Log.e("Bullet", "Không thể nạp sprite energy_bullet", e)
        }
    }

    fun update(particles: CopyOnWriteArrayList<Particle>) {
        x += speed

        // Sinh hạt tia lửa vàng phía sau viên đạn
        particles.add(
            Particle(
                x = x + 5f,
                y = y + size / 2f + (Random.nextFloat() * 10f - 5f),
                vx = -Random.nextFloat() * 3f - 2f,
                vy = Random.nextFloat() * 2f - 1f,
                color = Color.rgb(241, 196, 15),
                size = Random.nextFloat() * 5f + 2f,
                lifeSpan = 14
            )
        )
    }

    fun isOutOfScreen(screenWidth: Int): Boolean = x > screenWidth

    fun getHitbox(): RectF {
        return RectF(x, y, x + size, y + size)
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (sprite != null) {
            canvas.drawBitmap(sprite!!, x, y, null)
        } else {
            paint.color = Color.rgb(241, 196, 15)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x + size / 2f, y + size / 2f, size / 2f, paint)

            paint.color = Color.WHITE
            canvas.drawCircle(x + size / 2f, y + size / 2f, size * 0.25f, paint)
        }
    }
}
