package com.example.forestguardians

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

/**
 * GameButton - Thành phần nút bấm UI vẽ trực tiếp trên Canvas với hiệu ứng phát sáng.
 */
class GameButton(
    val id: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var text: String,
    var primaryColor: Int = Color.rgb(46, 204, 113),
    var textColor: Int = Color.WHITE,
    var hasPulseAnimation: Boolean = false
) {
    val bounds: RectF
        get() = RectF(x, y, x + width, y + height)

    private val btnPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    /**
     * Kiểm tra xem toạ độ ngón tay chạm vào có nằm trong phạm vi nút bấm không
     */
    fun isClicked(touchX: Float, touchY: Float): Boolean {
        return bounds.contains(touchX, touchY)
    }

    fun draw(canvas: Canvas, tick: Long = 0) {
        val pulse = if (hasPulseAnimation) (sin(tick * 0.1f) * 4f).toFloat() else 0f
        val drawRect = RectF(
            x - pulse,
            y - pulse,
            x + width + pulse,
            y + height + pulse
        )

        // 1. Nền mờ kính tối (Dark Glassmorphic background)
        btnPaint.style = Paint.Style.FILL
        btnPaint.color = Color.argb(220, 20, 32, 25)
        canvas.drawRoundRect(drawRect, 30f, 30f, btnPaint)

        // 2. Lớp màu chủ đạo bán trong suốt
        btnPaint.color = primaryColor
        btnPaint.alpha = 60
        canvas.drawRoundRect(drawRect, 30f, 30f, btnPaint)

        // 3. Viền sáng rực rỡ (Glowing Stroke)
        btnPaint.style = Paint.Style.STROKE
        btnPaint.strokeWidth = 4f
        btnPaint.color = primaryColor
        btnPaint.alpha = 230
        canvas.drawRoundRect(drawRect, 30f, 30f, btnPaint)

        // 4. Chữ hiển thị ở chính giữa nút
        textPaint.color = textColor
        textPaint.textSize = height * 0.42f
        val textY = y + (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, x + (width / 2f), textY, textPaint)
    }
}
