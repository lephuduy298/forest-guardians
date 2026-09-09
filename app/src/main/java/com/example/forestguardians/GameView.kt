package com.example.forestguardians

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/**
 * GameView - Bộ điều phối trung tâm của game (Controller & Game Loop).
 * Quản lý vòng đời SurfaceView, Thread Game Loop, điều phối trạng thái GameState và xử lý cảm ứng.
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

    // Kích thước màn hình thực tế
    private var screenWidth = 0
    private var screenHeight = 0
    private var isInitialized = false

    // Trạng thái game hiện tại (Mặc định bắt đầu từ Splash Screen)
    var currentState = GameState.SPLASH

    // Thống kê điểm số và mạng sống
    var score = 0
    var enemiesDefeated = 0
    var playerLives = 3

    // Các thực thể trong game (OOP)
    lateinit var player: Player
        private set
    lateinit var enemy: Enemy
        private set
    val bullets = CopyOnWriteArrayList<Bullet>()
    val particles = CopyOnWriteArrayList<Particle>()
    val fireflies = CopyOnWriteArrayList<Firefly>()

    // Các hệ thống con
    private var backgroundManager: BackgroundManager? = null
    val gameUI = GameUI(context)

    private val paint = Paint().apply { isAntiAlias = true }
    private var gameTick: Long = 0

    init {
        isFocusable = true
    }

    /**
     * Khởi tạo độ phân giải và các thực thể
     */
    private fun initGame(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height

        backgroundManager = BackgroundManager(context, width, height)
        gameUI.setupButtons(width, height)

        player = Player(context, x = 60f, y = (height - 160f) / 2f, size = 160f)
        enemy = Enemy(context, x = width - 200f, y = (height - 160f) / 2f, size = 160f)

        fireflies.clear()
        for (i in 0 until 30) {
            fireflies.add(Firefly(width, height))
        }

        isInitialized = true
        Log.d(TAG, "Đã khởi tạo GameView với màn hình ${width}x${height}")
    }

    /**
     * Bắt đầu một ván chơi mới
     */
    fun startNewGame() {
        score = 0
        enemiesDefeated = 0
        playerLives = 3
        bullets.clear()
        particles.clear()
        player.moveTo(60f, (screenHeight - player.size) / 2f)
        enemy.respawn(screenWidth, screenHeight)
        currentState = GameState.PLAYING
    }

    // ==========================================
    // SURFACEVIEW CALLBACKS & THREAD
    // ==========================================

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        initGame(width, height)
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
    // GAME LOOP CHÍNH (~60 FPS)
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
    // CẬP NHẬT LOGIC THEO TRẠNG THÁI (UPDATE)
    // ==========================================

    private fun updateGameLogic() {
        when (currentState) {
            GameState.SPLASH -> {
                // Chạy thanh loading bar từ 0% đến 100%
                if (gameUI.updateSplash()) {
                    currentState = GameState.MENU // Nạp xong tự chuyển sang Menu
                }
            }

            GameState.MENU, GameState.HOW_TO_PLAY, GameState.GAME_OVER -> {
                // Nền vẫn cuộn nhẹ và đom đóm vẫn bay
                backgroundManager?.update()
                for (firefly in fireflies) firefly.update(screenWidth, screenHeight)
                // Cập nhật các hạt nổ nếu còn sót lại
                for (particle in particles) {
                    particle.update()
                    if (particle.isDead()) particles.remove(particle)
                }
            }

            GameState.PLAYING -> {
                backgroundManager?.update()
                for (firefly in fireflies) firefly.update(screenWidth, screenHeight)

                // Cập nhật Tinh linh A
                player.update(gameTick, screenWidth, screenHeight, particles)

                // Cập nhật Robot B và kiểm tra lọt biên trái
                val crossedLeft = enemy.update(gameTick, screenWidth, screenHeight, particles)
                if (crossedLeft) {
                    playerLives--
                    createExplosion(50f, enemy.y, Color.rgb(231, 76, 60))
                    if (playerLives <= 0) {
                        currentState = GameState.GAME_OVER
                    }
                }

                // Cập nhật đạn C và kiểm tra va chạm với Robot B
                val enemyHitbox = enemy.getHitbox()
                for (bullet in bullets) {
                    bullet.update(particles)

                    if (bullet.isOutOfScreen(screenWidth)) {
                        bullets.remove(bullet)
                        continue
                    }

                    // Va chạm: Đạn C trúng Robot B
                    if (RectF.intersects(bullet.getHitbox(), enemyHitbox)) {
                        createExplosion(bullet.x, bullet.y, Color.rgb(241, 196, 15))
                        createExplosion(enemy.x + enemy.size / 2f, enemy.y + enemy.size / 2f, Color.rgb(231, 76, 60))

                        score += 100
                        enemiesDefeated++
                        bullets.remove(bullet)
                        enemy.respawn(screenWidth, screenHeight)
                        break
                    }
                }

                // Cập nhật hạt hiệu ứng
                for (particle in particles) {
                    particle.update()
                    if (particle.isDead()) particles.remove(particle)
                }
            }
        }
    }

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
                    lifeSpan = Random.nextInt(20, 38)
                )
            )
        }
    }

    // ==========================================
    // VẼ MÀN HÌNH THEO TRẠNG THÁI (RENDER)
    // ==========================================

    private fun drawGameScreen() {
        if (!surfaceHolder.surface.isValid) return

        var canvas: Canvas? = null
        try {
            canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                synchronized(surfaceHolder) {
                    renderByState(canvas)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi vẽ Canvas: ", e)
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi unlockCanvasAndPost: ", e)
                }
            }
        }
    }

    private fun renderByState(canvas: Canvas) {
        when (currentState) {
            GameState.SPLASH -> {
                gameUI.drawSplashScreen(canvas, screenWidth, screenHeight, gameTick)
            }

            GameState.MENU -> {
                // 1. Nền rừng cuộn êm dịu
                backgroundManager?.draw(canvas)
                // 2. Lớp phủ đen rêu mờ điện ảnh (75% tối) giúp giảm tối đa rối mắt, làm nổi bật Logo và Nút
                canvas.drawColor(Color.argb(195, 10, 18, 14))
                // 3. Đom đóm lơ lửng nhẹ nhàng trong màn đêm
                for (firefly in fireflies) firefly.draw(canvas, paint)
                // 4. Vẽ Logo và các nút bấm rõ nét, tương phản cao
                gameUI.drawMenuScreen(canvas, screenWidth, screenHeight, gameTick)
            }

            GameState.HOW_TO_PLAY -> {
                backgroundManager?.draw(canvas)
                canvas.drawColor(Color.argb(210, 10, 18, 14))
                for (firefly in fireflies) firefly.draw(canvas, paint)
                gameUI.drawHowToPlayDialog(canvas, screenWidth, screenHeight, gameTick)
            }

            GameState.PLAYING -> {
                // 1. Vẽ nền rừng cuộn
                backgroundManager?.draw(canvas)

                // 2. Lớp lọc màn đêm dịu mắt (Eye-Comfort Tint): giảm khoảng 45% độ chói,
                // mang lại tông rừng đêm trầm ấm, chống mỏi mắt và làm nổi bật đạn và nhân vật
                canvas.drawColor(Color.argb(125, 8, 18, 14))

                for (firefly in fireflies) firefly.draw(canvas, paint)
                for (particle in particles) particle.draw(canvas, paint)

                player.draw(canvas, paint, gameTick)
                enemy.draw(canvas, paint, gameTick)
                for (bullet in bullets) bullet.draw(canvas, paint)

                gameUI.drawInGameHUD(canvas, screenWidth, screenHeight, score, playerLives, bullets.size)
            }

            GameState.GAME_OVER -> {
                backgroundManager?.draw(canvas)
                for (particle in particles) particle.draw(canvas, paint)
                gameUI.drawGameOverScreen(canvas, screenWidth, screenHeight, score, enemiesDefeated, gameTick)
            }
        }
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN CẢM ỨNG (TOUCH EVENTS)
    // ==========================================

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDraggingPlayer = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (currentState) {
            GameState.SPLASH -> {
                // Nhấn vào màn hình splash để bỏ qua nhanh loading
                gameUI.loadingProgress = 100f
                currentState = GameState.MENU
                return true
            }

            GameState.MENU -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (gameUI.btnPlay?.isClicked(touchX, touchY) == true) {
                        startNewGame()
                        return true
                    }
                    if (gameUI.btnHowToPlay?.isClicked(touchX, touchY) == true) {
                        currentState = GameState.HOW_TO_PLAY
                        return true
                    }
                }
            }

            GameState.HOW_TO_PLAY -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (gameUI.btnCloseDialog?.isClicked(touchX, touchY) == true) {
                        currentState = GameState.MENU
                        return true
                    }
                }
            }

            GameState.PLAYING -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Nhấn nút Menu In-Game
                        if (gameUI.btnInGameMenu?.isClicked(touchX, touchY) == true) {
                            currentState = GameState.MENU
                            return true
                        }

                        // Kéo thả Tinh linh nếu chạm gần Tinh linh
                        if (touchX <= player.x + player.size + 80f &&
                            touchY >= player.y - 80f && touchY <= player.y + player.size + 80f) {
                            isDraggingPlayer = true
                            lastTouchX = touchX
                            lastTouchY = touchY
                        } else {
                            // Chạm bất kỳ đâu khác -> Bắn đạn
                            spawnBullet()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isDraggingPlayer) {
                            player.move(touchX - lastTouchX, touchY - lastTouchY)
                            lastTouchX = touchX
                            lastTouchY = touchY
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        isDraggingPlayer = false
                        return true
                    }
                }
            }

            GameState.GAME_OVER -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (gameUI.btnRestart?.isClicked(touchX, touchY) == true) {
                        startNewGame()
                        return true
                    }
                    if (gameUI.btnHome?.isClicked(touchX, touchY) == true) {
                        currentState = GameState.MENU
                        return true
                    }
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun spawnBullet() {
        val startX = player.x + player.size * 0.85f
        val startY = player.y + player.size * 0.45f + player.floatingY
        bullets.add(Bullet(context, startX, startY))

        // Tia lửa ở nòng bắn
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
}
