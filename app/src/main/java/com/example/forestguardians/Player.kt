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
import kotlin.math.sin
import kotlin.random.Random

/**
 * Player - Đối tượng A: Tinh linh Hộ vệ (Forest Spirit).
 * - Animation: Bay bồng bềnh hình sin, vòng hào quang phát sáng thở (pulsing aura), vệt bụi ma thuật.
 */
class Player(
    context: Context,
    var x: Float,
    var y: Float,
    val size: Float = 160f
) {
    var floatingY = 0f
    private var sprite: Bitmap? = null
    private var moveSpeed = 22f

    init {
        try {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.spirit_hero)
            if (raw != null) {
                sprite = Bitmap.createScaledBitmap(raw, size.toInt(), size.toInt(), true)
            }
        } catch (e: Exception) {
            Log.e("Player", "Không thể nạp sprite spirit_hero", e)
        }
    }

    // Các phương thức di chuyển linh hoạt
    fun move(dx: Float, dy: Float) {
        x += dx
        y += dy
    }

    fun moveUp() { y -= moveSpeed }
    fun moveDown() { y += moveSpeed }
    fun moveLeft() { x -= moveSpeed }
    fun moveRight() { x += moveSpeed }
    fun moveTo(targetX: Float, targetY: Float) { x = targetX; y = targetY }

    fun clampToBounds(screenWidth: Int, screenHeight: Int) {
        x = x.coerceIn(20f, screenWidth - size - 20f)
        y = y.coerceIn(20f, screenHeight - size - 20f)
    }

    fun update(tick: Long, screenWidth: Int, screenHeight: Int, particles: CopyOnWriteArrayList<Particle>) {
        clampToBounds(screenWidth, screenHeight)

        // Hoạt ảnh bay bồng bềnh (Sin wave floating)
        floatingY = sin(tick * 0.08f) * 12f

        // Sinh vệt bụi ma thuật xanh ngọc phía sau Tinh linh
        if (tick % 3 == 0L) {
            particles.add(
                Particle(
                    x = x + 25f,
                    y = y + floatingY + size / 2f + (Random.nextFloat() * 20f - 10f),
                    vx = -Random.nextFloat() * 3f - 1f,
                    vy = Random.nextFloat() * 2f - 1f,
                    color = Color.rgb(46, 204, 113),
                    size = Random.nextFloat() * 6f + 3f,
                    lifeSpan = 25
                )
            )
        }
    }

    fun getHitbox(): RectF {
        return RectF(x + 20f, y + floatingY + 20f, x + size - 20f, y + floatingY + size - 20f)
    }

    fun draw(canvas: Canvas, paint: Paint, tick: Long) {
        val drawY = y + floatingY

        // 1. Vòng hào quang phát quang (Pulsing Glow Aura)
        val pulse = (sin(tick * 0.1f) * 8f).toFloat()
        paint.color = Color.argb(55, 46, 204, 113)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x + size / 2f, drawY + size / 2f, size * 0.55f + pulse, paint)

        // 2. Vẽ Sprite nhân vật
        if (sprite != null) {
            canvas.drawBitmap(sprite!!, x, drawY, null)
        } else {
            paint.color = Color.rgb(46, 204, 113)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(x, drawY, x + size, drawY + size), 20f, 20f, paint)

            paint.color = Color.WHITE
            paint.textSize = 28f
            canvas.drawText("Player (A)", x + 20f, drawY + size / 2f, paint)
        }
    }
}
