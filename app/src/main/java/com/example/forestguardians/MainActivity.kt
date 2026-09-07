package com.example.forestguardians

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * MainActivity (Kotlin) - Màn hình chính điều khiển game "Forest Guardians".
 * Thiết lập chế độ Fullscreen Immersive và nhúng trực tiếp GameView.
 */
class MainActivity : ComponentActivity() {

    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Giữ cho màn hình luôn sáng khi đang chơi game
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Khởi tạo GameView và gán làm ContentView chính
        gameView = GameView(this)
        setContentView(gameView)

        // 3. Ẩn thanh điều hướng ảo và thanh trạng thái (Immersive Sticky Mode)
        hideSystemUI()
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        gameView?.resume() // Khôi phục Game Loop khi quay lại
    }

    override fun onPause() {
        super.onPause()
        gameView?.pause() // Tạm dừng Game Loop tránh hao pin và crash
    }
}
