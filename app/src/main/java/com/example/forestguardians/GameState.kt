package com.example.forestguardians

/**
 * GameState - Định nghĩa các trạng thái của luồng trò chơi (Game State Pattern).
 */
enum class GameState {
    SPLASH,       // Màn hình khởi động, nạp tài nguyên và thanh loading bar
    MENU,         // Màn hình trang chủ với logo và các nút bấm Play/Guide
    HOW_TO_PLAY,  // Hộp thoại popup hướng dẫn luật chơi chi tiết
    PLAYING,      // Trận chiến đang diễn ra
    GAME_OVER     // Hết máu (3 mạng), bảng điểm và nút chơi lại
}
