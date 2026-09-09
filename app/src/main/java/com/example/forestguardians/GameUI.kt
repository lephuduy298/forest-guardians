package com.example.forestguardians

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log

/**
 * GameUI - Quản lý vẽ toàn bộ giao diện: Splash (Loading bar), Main Menu, How to Play, In-game HUD, Game Over.
 */
class GameUI(val context: Context) {

    private var logoBitmap: Bitmap? = null

    // Quản lý tiến trình Loading (0f .. 100f)
    var loadingProgress = 0f
    private val loadingSpeed = 1.2f

    // Các nút bấm tương tác (GameButton)
    var btnPlay: GameButton? = null
    var btnHowToPlay: GameButton? = null
    var btnCloseDialog: GameButton? = null
    var btnRestart: GameButton? = null
    var btnHome: GameButton? = null
    var btnInGameMenu: GameButton? = null

    // Công cụ vẽ
    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { isAntiAlias = true }
    private val overlayPaint = Paint().apply { isAntiAlias = true }

    init {
        try {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.game_logo)
            if (raw != null) {
                logoBitmap = raw
            }
        } catch (e: Exception) {
            Log.e("GameUI", "Không thể nạp game_logo", e)
        }
    }

    /**
     * Cập nhật vị trí và kích thước các nút bấm theo độ phân giải màn hình
     */
    fun setupButtons(screenWidth: Int, screenHeight: Int) {
        val centerX = screenWidth / 2f

        val btnWidth = (screenWidth * 0.30f).coerceIn(260f, 360f)
        val btnHeight = 62f
        val btnX = centerX - btnWidth / 2f

        // Menu Buttons
        btnPlay = GameButton(
            id = "BTN_PLAY",
            x = btnX,
            y = screenHeight * 0.62f,
            width = btnWidth,
            height = btnHeight,
            text = "▶ BẮT ĐẦU CHƠI",
            primaryColor = Color.rgb(46, 204, 113),
            hasPulseAnimation = true
        )

        btnHowToPlay = GameButton(
            id = "BTN_HOW_TO_PLAY",
            x = btnX,
            y = screenHeight * 0.76f,
            width = btnWidth,
            height = btnHeight,
            text = "📖 HƯỚNG DẪN",
            primaryColor = Color.rgb(52, 152, 219)
        )

        btnCloseDialog = GameButton(
            id = "BTN_CLOSE_DIALOG",
            x = centerX - 120f,
            y = screenHeight * 0.76f,
            width = 240f,
            height = 60f,
            text = "✔ ĐÃ HIỂU",
            primaryColor = Color.rgb(46, 204, 113)
        )

        btnRestart = GameButton(
            id = "BTN_RESTART",
            x = centerX - 200f,
            y = screenHeight * 0.62f,
            width = 400f,
            height = 70f,
            text = "🔄 CHƠI LẠI NGAY",
            primaryColor = Color.rgb(46, 204, 113),
            hasPulseAnimation = true
        )

        btnHome = GameButton(
            id = "BTN_HOME",
            x = centerX - 200f,
            y = screenHeight * 0.75f,
            width = 400f,
            height = 65f,
            text = "🏠 VỀ TRANG CHỦ",
            primaryColor = Color.rgb(155, 89, 182)
        )

        btnInGameMenu = GameButton(
            id = "BTN_INGAME_MENU",
            x = screenWidth - 160f,
            y = 20f,
            width = 130f,
            height = 50f,
            text = "⏸ MENU",
            primaryColor = Color.rgb(52, 73, 94)
        )
    }

    // =========================================================================
    // 1. MÀN HÌNH KHỞI ĐỘNG (SPLASH SCREEN / LOADING BAR)
    // =========================================================================

    /**
     * @return true khi thanh loading đã nạp xong 100%
     */
    fun updateSplash(): Boolean {
        loadingProgress += loadingSpeed
        if (loadingProgress >= 100f) {
            loadingProgress = 100f
            return true
        }
        return false
    }

    fun drawSplashScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int, tick: Long) {
        // Nền rừng đêm tối
        canvas.drawColor(Color.rgb(18, 28, 22))

        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // Vẽ Logo ở vị trí trung tâm
        if (logoBitmap != null) {
            val logoWidth = screenWidth * 0.45f
            val ratio = logoWidth / logoBitmap!!.width.toFloat()
            val logoHeight = logoBitmap!!.height.toFloat() * ratio
            val logoDest = RectF(centerX - logoWidth / 2f, centerY - logoHeight * 0.8f, centerX + logoWidth / 2f, centerY + logoHeight * 0.2f)
            canvas.drawBitmap(logoBitmap!!, null, logoDest, null)
        }

        // --- VẼ THANH LOADING BAR ---
        val barWidth = screenWidth * 0.5f
        val barHeight = 22f
        val barX = centerX - barWidth / 2f
        val barY = centerY + 90f

        // 1. Khung nền thanh tiến trình (Đen mờ)
        paint.color = Color.argb(180, 20, 30, 25)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(barX, barY, barX + barWidth, barY + barHeight), 12f, 12f, paint)

        // 2. Viền ngoài
        paint.color = Color.rgb(46, 204, 113)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(RectF(barX, barY, barX + barWidth, barY + barHeight), 12f, 12f, paint)

        // 3. Phần trăm tiến trình (Màu xanh ngọc rực rỡ)
        paint.style = Paint.Style.FILL
        val progressWidth = barWidth * (loadingProgress / 100f)
        if (progressWidth > 0) {
            canvas.drawRoundRect(RectF(barX, barY, barX + progressWidth, barY + barHeight), 12f, 12f, paint)
        }

        // 4. Dòng trạng thái nạp thú vị
        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
        val statusText = when {
            loadingProgress < 35f -> "Đang đánh thức rừng thiêng... ${loadingProgress.toInt()}%"
            loadingProgress < 75f -> "Đang sạc quả cầu năng lượng... ${loadingProgress.toInt()}%"
            else -> "Robot xâm lăng đang đến gần! Sẵn sàng... 100%"
        }
        canvas.drawText(statusText, centerX, barY + barHeight + 35f, textPaint)
    }

    // =========================================================================
    // 2. MÀN HÌNH TRANG CHỦ (HOME SCREEN / MAIN MENU)
    // =========================================================================

    fun drawMenuScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int, tick: Long) {
        val centerX = screenWidth / 2f

        // Vẽ Logo chính của Game với tỉ lệ cân đối, hào quang nhẹ
        if (logoBitmap != null) {
            val logoWidth = (screenWidth * 0.38f).coerceIn(320f, 500f)
            val ratio = logoWidth / logoBitmap!!.width.toFloat()
            val logoHeight = logoBitmap!!.height.toFloat() * ratio
            val logoTop = screenHeight * 0.08f
            val logoDest = RectF(centerX - logoWidth / 2f, logoTop, centerX + logoWidth / 2f, logoTop + logoHeight)

            // Vầng sáng ngọc bích mờ dịu giúp Logo tách biệt tuyệt đối khỏi nền
            paint.color = Color.argb(45, 46, 204, 113)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, logoTop + logoHeight / 2f, logoWidth * 0.44f, paint)

            canvas.drawBitmap(logoBitmap!!, null, logoDest, null)
        }

        // Vẽ các nút bấm tương tác
        btnPlay?.draw(canvas, tick)
        btnHowToPlay?.draw(canvas, tick)

        // Thông tin bản quyền đồ án ở chân trang
        textPaint.color = Color.argb(160, 255, 255, 255)
        textPaint.textSize = 20f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Đồ án Lập Trình Game 2D Android • Forest Guardians v1.0", centerX, screenHeight - 20f, textPaint)
    }

    // =========================================================================
    // 3. HỘP THOẠI HƯỚNG DẪN (HOW TO PLAY POPUP)
    // =========================================================================

    fun drawHowToPlayDialog(canvas: Canvas, screenWidth: Int, screenHeight: Int, tick: Long) {
        // Lớp phủ nền tối làm mờ màn hình sau
        overlayPaint.color = Color.argb(200, 10, 16, 12)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlayPaint)

        val centerX = screenWidth / 2f
        val dialogWidth = screenWidth * 0.65f
        val dialogHeight = screenHeight * 0.75f
        val dialogLeft = centerX - dialogWidth / 2f
        val dialogTop = (screenHeight - dialogHeight) / 2f
        val dialogRect = RectF(dialogLeft, dialogTop, dialogLeft + dialogWidth, dialogTop + dialogHeight)

        // Khung hộp thoại kính mờ
        paint.color = Color.argb(240, 24, 38, 30)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(dialogRect, 30f, 30f, paint)

        // Viền xanh ngọc
        paint.color = Color.rgb(46, 204, 113)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(dialogRect, 30f, 30f, paint)

        // Tiêu đề
        textPaint.color = Color.rgb(255, 215, 0)
        textPaint.textSize = 36f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("📖 HƯỚNG DẪN BẢO VỆ RỪNG", centerX, dialogTop + 55f, textPaint)

        // Các dòng nội dung hướng dẫn
        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.isFakeBoldText = false
        textPaint.textAlign = Paint.Align.LEFT
        val lineX = dialogLeft + 45f
        var startY = dialogTop + 115f

        canvas.drawText("1. CHẠM MÀN HÌNH: Tinh linh bắn đạn năng lượng về phía trước.", lineX, startY, textPaint)
        startY += 42f
        canvas.drawText("2. KÉO NGÓN TAY: Chạm giữ gần Tinh linh để né đạn và bay tự do.", lineX, startY, textPaint)
        startY += 42f
        canvas.drawText("3. TIÊU DIỆT ROBOT: Bắn trúng Robot để ghi +100 điểm thưởng.", lineX, startY, textPaint)
        startY += 42f
        canvas.drawText("4. GIỮ VỮNG PHÒNG TUYẾN: Robot lọt qua biên trái bạn sẽ mất 1 mạng ❤️.", lineX, startY, textPaint)
        startY += 42f
        canvas.drawText("5. Bạn có 3 MẠNG sống. Hết mạng trò chơi sẽ kết thúc!", lineX, startY, textPaint)

        // Nút đóng hộp thoại
        btnCloseDialog?.draw(canvas, tick)
    }

    // =========================================================================
    // 4. GIAO DIỆN TRONG TRẬN ĐẤU (IN-GAME HUD + 3 MẠNG)
    // =========================================================================

    fun drawInGameHUD(canvas: Canvas, screenWidth: Int, screenHeight: Int, score: Int, lives: Int, bulletsCount: Int) {
        // Bảng điểm số mờ bo góc
        paint.color = Color.argb(170, 15, 23, 20)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(30f, 20f, 540f, 95f), 22f, 22f, paint)

        paint.color = Color.argb(200, 46, 204, 113)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(RectF(30f, 20f, 540f, 95f), 22f, 22f, paint)

        // Điểm số
        textPaint.color = Color.rgb(255, 215, 0)
        textPaint.textSize = 32f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("★ ĐIỂM SỐ: $score", 50f, 58f, textPaint)

        // Mạng sống dạng biểu tượng trái tim
        val heartsStr = buildString {
            for (i in 0 until lives) append("❤️ ")
            for (i in lives until 3) append("🖤 ")
        }
        textPaint.color = Color.WHITE
        textPaint.textSize = 22f
        textPaint.isFakeBoldText = false
        canvas.drawText("Mạng: $heartsStr  |  Đạn: $bulletsCount", 50f, 85f, textPaint)

        // Nút Menu In-Game
        btnInGameMenu?.draw(canvas)
    }

    // =========================================================================
    // 5. MÀN HÌNH GAME OVER (KẾT THÚC)
    // =========================================================================

    fun drawGameOverScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int, finalScore: Int, enemiesDefeated: Int, tick: Long) {
        // Phủ nền đỏ mờ cảnh báo
        overlayPaint.color = Color.argb(220, 35, 12, 12)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlayPaint)

        val centerX = screenWidth / 2f

        // Tiêu đề Game Over
        textPaint.color = Color.rgb(231, 76, 60)
        textPaint.textSize = 55f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("☠ RỪNG ĐÃ BỊ XÂM CHIẾM ☠", centerX, screenHeight * 0.28f, textPaint)

        // Điểm số đạt được
        textPaint.color = Color.rgb(255, 215, 0)
        textPaint.textSize = 38f
        canvas.drawText("TỔNG ĐIỂM CỦA BẠN: $finalScore", centerX, screenHeight * 0.40f, textPaint)

        // Số Robot đã hạ gục
        textPaint.color = Color.WHITE
        textPaint.textSize = 26f
        textPaint.isFakeBoldText = false
        canvas.drawText("Robot xâm lăng đã bị tiêu diệt: $enemiesDefeated", centerX, screenHeight * 0.48f, textPaint)

        // Các nút bấm Chơi lại / Về trang chủ
        btnRestart?.draw(canvas, tick)
        btnHome?.draw(canvas, tick)
    }
}
