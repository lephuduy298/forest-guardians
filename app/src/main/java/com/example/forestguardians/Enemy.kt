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
import kotlin.math.cos
import kotlin.random.Random

/**
 * Enemy - Đối tượng B: Robot Xâm Lăng (Combat Drone).
 * - Animation: Lắc lư cơ học, luồng lửa và khói phản lực, mắt đỏ radar.
 */
class Enemy(
    context: Context,
    var x: Float,
    var y: Float,
    val size: Float = 160f
) {
    var speed = 7.5f
    private var sprite: Bitmap? = null
    private var hoverOffset = 0f

    init {
        try {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.robot_enemy)
            if (raw != null) {
                sprite = Bitmap.createScaledBitmap(raw, size.toInt(), size.toInt(), true)
            }
        } catch (e: Exception) {
            Log.e("Enemy", "Không thể nạp sprite robot_enemy", e)
        }
    }

    /**
     * Cập nhật vị trí B.
     * @return true nếu Robot vượt qua biên trái màn hình (để trừ máu người chơi).
     */
    fun update(tick: Long, screenWidth: Int, screenHeight: Int, particles: CopyOnWriteArrayList<Particle>): Boolean {
        x -= speed
        hoverOffset = cos(tick * 0.12f) * 6f

        // Luồng lửa phản lực màu cam/đỏ phun ra phía sau (bên phải robot)
        if (tick % 2 == 0L) {
            particles.add(
                Particle(
                    x = x + size * 0.75f,
                    y = y + hoverOffset + size * 0.75f,
                    vx = Random.nextFloat() * 4f + 2f,
                    vy = Random.nextFloat() * 4f + 1f,
                    color = if (Random.nextBoolean()) Color.rgb(255, 100, 0) else Color.rgb(231, 76, 60),
                    size = Random.nextFloat() * 7f + 3f,
                    lifeSpan = 18
                )
            )
        }

        // Kiểm tra chạm biên trái màn hình (toạ độ X <= 0)
        if (x + size <= 0 || x <= 0) {
            respawn(screenWidth, screenHeight)
            return true // Đã lọt qua biên trái
        }

        return false
    }

    fun respawn(screenWidth: Int, screenHeight: Int) {
        this.x = screenWidth.toFloat()
        val maxY = (screenHeight - size - 80).toInt().coerceAtLeast(1)
        this.y = (Random.nextInt(maxY) + 40).toFloat()
    }

    fun getHitbox(): RectF {
        return RectF(x + 25f, y + hoverOffset + 25f, x + size - 25f, y + hoverOffset + size - 25f)
    }

    fun draw(canvas: Canvas, paint: Paint, tick: Long) {
        val drawY = y + hoverOffset

        // Vầng hào quang đỏ cảnh báo
        paint.color = Color.argb(40, 231, 76, 60)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x + size / 2f, drawY + size / 2f, size * 0.52f, paint)

        // Vẽ Sprite Robot
        if (sprite != null) {
            canvas.drawBitmap(sprite!!, x, drawY, null)
        } else {
            paint.color = Color.rgb(231, 76, 60)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(x, drawY, x + size, drawY + size), 20f, 20f, paint)

            paint.color = Color.WHITE
            paint.textSize = 28f
            canvas.drawText("Robot (B)", x + 20f, drawY + size / 2f, paint)
        }
    }
}
