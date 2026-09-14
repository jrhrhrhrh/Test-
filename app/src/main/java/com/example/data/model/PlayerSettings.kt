package com.example.data.model

data class PlayerSettings(
    val defaultSpeed: Float = 1.0f,
    val seekDurationSeconds: Int = 10,
    val autoHideControlsSeconds: Int = 4,
    val resizeMode: Int = 0, // 0 = Fit, 1 = Fill, 2 = Zoom
    val rememberPlaybackPosition: Boolean = true,
    val autoPlayNext: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val themeMode: String = "system", // "system", "dark", "light"
    val subtitleSizeSp: Float = 16f,
    val subtitleBackground: Boolean = true,
    val loopMode: Int = 0 // 0 = Off, 1 = Repeat All, 2 = Repeat One
)
