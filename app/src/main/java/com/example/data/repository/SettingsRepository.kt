package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PlayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("video_player_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PlayerSettings> = _settings.asStateFlow()

    private fun loadSettings(): PlayerSettings {
        return PlayerSettings(
            defaultSpeed = prefs.getFloat(KEY_SPEED, 1.0f),
            seekDurationSeconds = prefs.getInt(KEY_SEEK_DURATION, 10),
            autoHideControlsSeconds = prefs.getInt(KEY_AUTO_HIDE, 4),
            resizeMode = prefs.getInt(KEY_RESIZE_MODE, 0),
            rememberPlaybackPosition = prefs.getBoolean(KEY_REMEMBER_POS, true),
            autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true),
            gesturesEnabled = prefs.getBoolean(KEY_GESTURES_ENABLED, true),
            seekGestureEnabled = prefs.getBoolean(KEY_SEEK_GESTURE, true),
            brightnessGestureEnabled = prefs.getBoolean(KEY_BRIGHTNESS_GESTURE, true),
            volumeGestureEnabled = prefs.getBoolean(KEY_VOLUME_GESTURE, true),
            backgroundPlayback = prefs.getBoolean(KEY_BG_PLAYBACK, true),
            themeMode = prefs.getString(KEY_THEME_MODE, "system") ?: "system",
            subtitleSizeSp = prefs.getFloat(KEY_SUBTITLE_SIZE, 16f),
            subtitleBackground = prefs.getBoolean(KEY_SUBTITLE_BG, true),
            loopMode = prefs.getInt(KEY_LOOP_MODE, 0)
        )
    }

    fun updateSettings(newSettings: PlayerSettings) {
        prefs.edit()
            .putFloat(KEY_SPEED, newSettings.defaultSpeed)
            .putInt(KEY_SEEK_DURATION, newSettings.seekDurationSeconds)
            .putInt(KEY_AUTO_HIDE, newSettings.autoHideControlsSeconds)
            .putInt(KEY_RESIZE_MODE, newSettings.resizeMode)
            .putBoolean(KEY_REMEMBER_POS, newSettings.rememberPlaybackPosition)
            .putBoolean(KEY_AUTO_PLAY_NEXT, newSettings.autoPlayNext)
            .putBoolean(KEY_GESTURES_ENABLED, newSettings.gesturesEnabled)
            .putBoolean(KEY_SEEK_GESTURE, newSettings.seekGestureEnabled)
            .putBoolean(KEY_BRIGHTNESS_GESTURE, newSettings.brightnessGestureEnabled)
            .putBoolean(KEY_VOLUME_GESTURE, newSettings.volumeGestureEnabled)
            .putBoolean(KEY_BG_PLAYBACK, newSettings.backgroundPlayback)
            .putString(KEY_THEME_MODE, newSettings.themeMode)
            .putFloat(KEY_SUBTITLE_SIZE, newSettings.subtitleSizeSp)
            .putBoolean(KEY_SUBTITLE_BG, newSettings.subtitleBackground)
            .putInt(KEY_LOOP_MODE, newSettings.loopMode)
            .apply()
        _settings.value = newSettings
    }

    companion object {
        private const val KEY_SPEED = "speed"
        private const val KEY_SEEK_DURATION = "seek_duration"
        private const val KEY_AUTO_HIDE = "auto_hide"
        private const val KEY_RESIZE_MODE = "resize_mode"
        private const val KEY_REMEMBER_POS = "remember_pos"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_GESTURES_ENABLED = "gestures_enabled"
        private const val KEY_SEEK_GESTURE = "seek_gesture"
        private const val KEY_BRIGHTNESS_GESTURE = "brightness_gesture"
        private const val KEY_VOLUME_GESTURE = "volume_gesture"
        private const val KEY_BG_PLAYBACK = "bg_playback"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SUBTITLE_SIZE = "subtitle_size"
        private const val KEY_SUBTITLE_BG = "subtitle_bg"
        private const val KEY_LOOP_MODE = "loop_mode"
    }
}
