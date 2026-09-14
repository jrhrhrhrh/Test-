package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VideoRepository
import com.example.player.MediaPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VideoPlayerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var repository: VideoRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var playerManager: MediaPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        repository = VideoRepository(
            context = this,
            videoDao = database.videoDao(),
            playlistDao = database.playlistDao(),
            recentUrlDao = database.recentUrlDao()
        )
        settingsRepository = SettingsRepository(this)
        playerManager = MediaPlayerManager(this)

        playerManager.applySettings(settingsRepository.settings.value)

        playerManager.onProgressUpdateListener = { uri, pos, dur ->
            applicationScope.launch {
                repository.updatePlaybackProgress(uri, pos, dur)
            }
        }
    }

    companion object {
        lateinit var instance: VideoPlayerApplication
            private set
    }
}
