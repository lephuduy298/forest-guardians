package com.example.forestguardians

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * GameView (Phiên bản chuyên nghiệp phục vụ Báo cáo tuần)
 * Tích hợp:
 * - Cuộn nền vô tận (Infinite Parallax Scrolling)
 * - Đom đóm & bụi ánh sáng ma thuật (Ambient Fireflies)
 * - Animation Tinh linh A (Bay bồng bềnh + hào quang + vệt sáng)
 * - Animation Robot B (Rung cơ học + lửa phản lực + mắt radar)
 * - Đạn C năng lượng xoay sáng & particle trail
 * - Hệ thống va chạm (Collision Detection) & Nổ hạt rực rỡ (Particle Explosion)
 * - Giao diện HUD điểm số hiện đại
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    companion object {
        private const val TAG = "GameView"
        private const val TARGET_FPS = 60
        private const val TARGET_TIME = 1000L / TARGET_FPS
    }

    private val surfaceHolder: SurfaceHolder = holder.apply { addCallback(this@GameView) }
    private var gameThread: Thread? = null

    @Volatile
    private var isPlaying = false

    private var screenWidth = 0
    private var screenHeight = 0
    private var isInitialized = false

    // Điểm số và thống kê
    private var score = 0
    private var enemiesDefeated = 0

    // Các thực thể trong game
    lateinit var player: Player
        private set
    lateinit var enemy: Enemy
        private set
    val bullets = CopyOnWriteArrayList<Bullet>()
    val particles = CopyOnWriteArrayList<Particle>()
    val fireflies = CopyOnWriteArrayList<Firefly>()

    // Quản lý nền cuộn vô tận
    private var backgroundManager: BackgroundManager? = null

    // Công cụ vẽ
    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { isAntiAlias = true }
    private val hudBgPaint = Paint().apply { isAntiAlias = true }

    // Biến thời gian phục vụ animation
    private var gameTick: Long = 0

    init {
        isFocusable = true
    }

    /**
     * Khởi tạo các đối tượng dựa trên độ phân giải thực tế của màn hình
     */
    private fun initGameEntities(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height

        // 1. Nạp và khởi tạo nền cuộn vô tận
        backgroundManager = BackgroundManager(context, width, height)

        // 2. Khởi tạo đối tượng A (Tinh linh): 160x160, chính giữa mép trái
        player = Player(context, x = 40f, y = (height - 160f) / 2f, size = 160f)

        // 3. Khởi tạo đối tượng B (Robot): 160x160, chính giữa mép phải đối diện A
        enemy = Enemy(context, x = width - 200f, y = (height - 160f) / 2f, size = 160f)

        // 4. Sinh 30 đom đóm ma thuật bay lượn trong rừng
        fireflies.clear()
        for (i in 0 until 30) {
            fireflies.add(Firefly(width, height))
        }

        isInitialized = true
        Log.d(TAG, "Đã khởi tạo game chuyên nghiệp thành công: ${width}x${height}")
    }

    // ==========================================
    // SURFACEVIEW CALLBACKS & THREAD
    // ==========================================

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        initGameEntities(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    fun resume() {
        if (!isPlaying) {
            isPlaying = true
            gameThread = Thread(this).apply { start() }
        }
    }

    fun pause() {
        isPlaying = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Lỗi dừng Game Thread", e)
        }
    }

    // ==========================================
    // GAME LOOP (VÒNG LẶP CHÍNH ~60 FPS)
    // ==========================================

    override fun run() {
        while (isPlaying) {
            val startTime = System.currentTimeMillis()

            if (isInitialized) {
                gameTick++
                updateGameLogic()
                drawGameScreen()
            }

            val timeTaken = System.currentTimeMillis() - startTime
            val sleepTime = TARGET_TIME - timeTaken
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime)
                } catch (_: InterruptedException) { }
            }
        }
    }

    // ==========================================
    // CẬP NHẬT LOGIC GAME & VA CHẠM
    // ==========================================

    private fun updateGameLogic() {
        // 1. Cập nhật nền cuộn vô tận
        backgroundManager?.update()

        // 2. Cập nhật đom đóm
        for (firefly in fireflies) {
            firefly.update(screenWidth, screenHeight)
        }

        // 3. Cập nhật Tinh linh A (Animation bay lơ lửng, tạo vệt sáng)
        player.update(gameTick, screenWidth, screenHeight, particles)

        // 4. Cập nhật Robot B (Di chuyển từ phải sang trái, tạo lửa phản lực)
        enemy.update(gameTick, screenWidth, screenHeight, particles)

        // 5. Cập nhật danh sách đạn C và kiểm tra va chạm với Robot B
        val enemyHitbox = enemy.getHitbox()

        for (bullet in bullets) {
            bullet.update(particles)

            // Kiểm tra đạn bay khỏi màn hình bên phải
            if (bullet.isOutOfScreen(screenWidth)) {
                bullets.remove(bullet)
                continue
            }

            // XỬ LÝ VA CHẠM (COLLISION): Đạn C trúng Robot B
            if (RectF.intersects(bullet.getHitbox(), enemyHitbox)) {
                // Tạo vụ nổ hạt sáng rực rỡ
                createExplosion(bullet.x, bullet.y, Color.rgb(241, 196, 15)) // Nổ vàng cam
                createExplosion(enemy.x + enemy.size / 2f, enemy.y + enemy.size / 2f, Color.rgb(231, 76, 60)) // Nổ đỏ

                // Tăng điểm
                score += 100
                enemiesDefeated++

                // Xoá viên đạn
                bullets.remove(bullet)

                // Hồi sinh B về mép phải với Y ngẫu nhiên
                enemy.respawn(screenWidth, screenHeight)
                break
            }
        }

        // 6. Cập nhật các hạt hiệu ứng (Particles)
        for (particle in particles) {
            particle.update()
            if (particle.isDead()) {
                particles.remove(particle)
            }
        }
    }

    /**
     * Tạo vụ nổ hạt sáng toé ra các hướng khi trúng mục tiêu
     */
    private fun createExplosion(x: Float, y: Float, baseColor: Int) {
        for (i in 0 until 25) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = Random.nextFloat() * 14f - 7f,
                    vy = Random.nextFloat() * 14f - 7f,
                    color = baseColor,
                    size = Random.nextFloat() * 8f + 4f,
                    lifeSpan = Random.nextInt(20, 40)
                )
            )
        }
    }

    // ==========================================
    // VẼ TOÀN BỘ ĐỒ HỌA GAME (RENDER)
    // ==========================================

    private fun drawGameScreen() {
        if (!surfaceHolder.surface.isValid) return

        var canvas: Canvas? = null
        try {
            canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                synchronized(surfaceHolder) {
                    renderFrame(canvas)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi vẽ Canvas", e)
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi unlockCanvasAndPost", e)
                }
            }
        }
    }

    private fun renderFrame(canvas: Canvas) {
        // 1. Vẽ nền cuộn vô tận
        if (backgroundManager != null) {
            backgroundManager!!.draw(canvas)
        } else {
            canvas.drawColor(Color.rgb(27, 38, 29))
        }

        // 2. Vẽ đom đóm ma thuật mờ ảo
        for (firefly in fireflies) {
            firefly.draw(canvas, paint)
        }

        // 3. Vẽ các hạt hiệu ứng (khói phản lực, vệt sáng, vụ nổ)
        for (particle in particles) {
            particle.draw(canvas, paint)
        }

        // 4. Vẽ Đối tượng A (Tinh linh Hộ vệ)
        player.draw(canvas, paint, gameTick)

        // 5. Vẽ Đối tượng B (Robot Xâm lăng)
        enemy.draw(canvas, paint, gameTick)

        // 6. Vẽ tất cả viên đạn C (Quả cầu năng lượng xoáy sáng)
        for (bullet in bullets) {
            bullet.draw(canvas, paint)
        }

        // 7. Vẽ giao diện HUD hiện đại (Điểm số, hướng dẫn)
        drawModernHUD(canvas)
    }

    /**
     * Vẽ thanh điểm số HUD phong cách Glassmorphism
     */
    private fun drawModernHUD(canvas: Canvas) {
        // Khung nền đen mờ bo tròn phía trên
        hudBgPaint.color = Color.argb(170, 15, 23, 20)
        canvas.drawRoundRect(RectF(30f, 20f, 480f, 100f), 25f, 25f, hudBgPaint)

        // Viền xanh ngọc tinh tế
        hudBgPaint.style = Paint.Style.STROKE
        hudBgPaint.strokeWidth = 3f
        hudBgPaint.color = Color.argb(200, 46, 204, 113)
        canvas.drawRoundRect(RectF(30f, 20f, 480f, 100f), 25f, 25f, hudBgPaint)
        hudBgPaint.style = Paint.Style.FILL

        // Chữ Điểm số (Vàng ánh kim)
        textPaint.color = Color.rgb(255, 215, 0)
        textPaint.textSize = 34f
        textPaint.isFakeBoldText = true
        canvas.drawText("★ ĐIỂM SỐ: $score", 55f, 60f, textPaint)

        // Chữ Robot hạ gục
        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.isFakeBoldText = false
        canvas.drawText("Robot tiêu diệt: $enemiesDefeated  |  Đạn: ${bullets.size}", 55f, 88f, textPaint)

        // Dòng hướng dẫn mờ ở góc dưới
        textPaint.color = Color.argb(160, 255, 255, 255)
        textPaint.textSize = 26f
        canvas.drawText(" chạm màn hình để bắn • kéo ngón tay để di chuyển Tinh linh", 40f, screenHeight - 30f, textPaint)
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN CẢM ỨNG & KÉO DI CHUYỂN
    // ==========================================

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDraggingPlayer = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInitialized) {
                    val touchX = event.x
                    val touchY = event.y

                    // Nếu chạm gần Tinh linh A -> Kích hoạt chế độ kéo thả di chuyển
                    if (touchX <= player.x + player.size + 80f &&
                        touchY >= player.y - 80f && touchY <= player.y + player.size + 80f) {
                        isDraggingPlayer = true
                        lastTouchX = touchX
                        lastTouchY = touchY
                    } else {
                        // Chạm bất kỳ đâu trên màn hình -> Bắn đạn C
                        spawnBullet()
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingPlayer) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    player.move(deltaX, deltaY)
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDraggingPlayer) {
                    // Nhấp nhẹ cũng bắn đạn
                }
                isDraggingPlayer = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun spawnBullet() {
        val startX = player.x + player.size * 0.85f
        val startY = player.y + player.size * 0.45f + player.floatingY
        bullets.add(Bullet(context, startX, startY))

        // Tạo hiệu ứng hạt sáng lóe lên ở nòng bắn
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    x = startX,
                    y = startY,
                    vx = Random.nextFloat() * 6f + 2f,
                    vy = Random.nextFloat() * 6f - 3f,
                    color = Color.rgb(0, 229, 255),
                    size = 5f,
                    lifeSpan = 15
                )
            )
        }
    }

    // =========================================================================
    // HỆ THỐNG NỀN CUỘN VÔ TẬN (INFINITE PARALLAX SCROLLING)
    // =========================================================================

    class BackgroundManager(context: Context, val screenWidth: Int, val screenHeight: Int) {
        private var bgBitmap: Bitmap? = null
        private var bgX1 = 0f
        private var bgX2 = 0f
        private val scrollSpeed = 3.5f // Tốc độ cuộn nền sang trái
        private var scaledBgWidth = screenWidth

        init {
            try {
                val original = BitmapFactory.decodeResource(context.resources, R.drawable.bg_forest)
                if (original != null) {
                    // Tính toán tỉ lệ để chiều cao hình vừa khít chiều cao màn hình
                    val ratio = screenHeight.toFloat() / original.height.toFloat()
                    scaledBgWidth = (original.width * ratio).toInt()
                    // Đảm bảo chiều rộng nền tối thiểu bằng màn hình
                    if (scaledBgWidth < screenWidth) scaledBgWidth = screenWidth

                    bgBitmap = Bitmap.createScaledBitmap(original, scaledBgWidth, screenHeight, true)
                    bgX1 = 0f
                    bgX2 = scaledBgWidth.toFloat()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Không thể tải bg_forest", e)
            }
        }

        fun update() {
            bgX1 -= scrollSpeed
            bgX2 -= scrollSpeed

            // Khi tấm ảnh 1 cuộn hết về bên trái, đặt nó nối tiếp sau tấm 2
            if (bgX1 + scaledBgWidth <= 0) {
                bgX1 = bgX2 + scaledBgWidth
            }
            // Khi tấm ảnh 2 cuộn hết về bên trái, đặt nó nối tiếp sau tấm 1
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

    // =========================================================================
    // CÁC LỚP THỰC THỂ NÂNG CAO (OOP ENTITIES VỚI ANIMATION & SPRITES)
    // =========================================================================

    /**
     * Đối tượng A: Tinh linh Hộ vệ (Forest Spirit)
     * - Animation: Bay bồng bềnh hình sin, hào quang phát sáng thở (pulsing aura), vệt lá phát sáng
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
                Log.e(TAG, "Không thể tải sprite spirit_hero", e)
            }
        }

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

            // Tạo vệt bụi ma thuật xanh ngọc phía sau Tinh linh (mỗi 3 frames sinh 1 hạt)
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

            // 1. Vẽ vòng hào quang phát sáng (Pulsing Glow Aura)
            val pulse = (sin(tick * 0.1f) * 8f).toFloat()
            paint.color = Color.argb(55, 46, 204, 113)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x + size / 2f, drawY + size / 2f, size * 0.55f + pulse, paint)

            // 2. Vẽ Sprite nhân vật (hoặc vẽ Fallback nếu chưa tải xong ảnh)
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

    /**
     * Đối tượng B: Robot Xâm lăng (Combat Drone)
     * - Animation: Rung cơ học, luồng lửa phản lực ở đuôi, mắt radar đỏ quét
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
                Log.e(TAG, "Không thể tải sprite robot_enemy", e)
            }
        }

        fun update(tick: Long, screenWidth: Int, screenHeight: Int, particles: CopyOnWriteArrayList<Particle>) {
            // Di chuyển liên tục từ phải sang trái
            x -= speed

            // Rung lắc cơ học nhẹ
            hoverOffset = cos(tick * 0.12f) * 6f

            // Tạo luồng khói và lửa phản lực màu cam/đỏ phun ra phía sau (bên phải robot)
            if (tick % 2 == 0L) {
                particles.add(
                    Particle(
                        x = x + size * 0.75f,
                        y = y + hoverOffset + size * 0.75f,
                        vx = Random.nextFloat() * 4f + 2f, // Phun về sau
                        vy = Random.nextFloat() * 4f + 1f, // Hướng xuống
                        color = if (Random.nextBoolean()) Color.rgb(255, 100, 0) else Color.rgb(231, 76, 60),
                        size = Random.nextFloat() * 7f + 3f,
                        lifeSpan = 18
                    )
                )
            }

            // Tái sinh khi chạm mép trái màn hình (toạ độ X <= 0)
            if (x + size <= 0 || x <= 0) {
                respawn(screenWidth, screenHeight)
            }
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

            // 1. Quầng sáng đỏ nguy hiểm bao quanh Robot
            paint.color = Color.argb(40, 231, 76, 60)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x + size / 2f, drawY + size / 2f, size * 0.52f, paint)

            // 2. Vẽ Sprite Robot
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

    /**
     * Đối tượng C: Quả cầu năng lượng xoáy sáng
     */
    class Bullet(
        context: Context,
        var x: Float,
        var y: Float,
        val size: Float = 55f
    ) {
        val speed = 24f // Bay nhanh hơn B rõ rệt
        private var sprite: Bitmap? = null

        init {
            try {
                val raw = BitmapFactory.decodeResource(context.resources, R.drawable.energy_bullet)
                if (raw != null) {
                    sprite = Bitmap.createScaledBitmap(raw, size.toInt(), size.toInt(), true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Không thể tải sprite energy_bullet", e)
            }
        }

        fun update(particles: CopyOnWriteArrayList<Particle>) {
            x += speed

            // Vệt đuôi tia lửa vàng sau viên đạn
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

    // =========================================================================
    // HỆ THỐNG HẠT HIỆU ỨNG (PARTICLE SYSTEM)
    // =========================================================================

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
            size = (size * 0.96f).coerceAtLeast(1f)
        }

        fun isDead(): Boolean = lifeSpan <= 0

        fun draw(canvas: Canvas, paint: Paint) {
            val alpha = ((lifeSpan.toFloat() / maxLife) * 255).toInt().coerceIn(0, 255)
            paint.color = color
            paint.alpha = alpha
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, size, paint)
            paint.alpha = 255 // Reset alpha
        }
    }

    /**
     * Đom đóm & bụi ánh sáng ma thuật bay lơ lửng trong rừng
     */
    class Firefly(val screenWidth: Int, val screenHeight: Int) {
        var x = Random.nextFloat() * screenWidth
        var y = Random.nextFloat() * screenHeight
        var vx = Random.nextFloat() * 1.5f - 0.75f
        var vy = Random.nextFloat() * 1.5f - 0.75f
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
            paint.color = Color.rgb(180, 255, 200)
            paint.alpha = pulse
            canvas.drawCircle(x, y, size, paint)
            paint.alpha = 255
        }
    }
}
