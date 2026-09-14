package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.VideoPlayerApplication
import com.example.data.model.PlayerSettings
import com.example.data.model.VideoItem
import com.example.player.AudioTrackInfo
import com.example.player.MediaPlayerManager
import com.example.player.SubtitleTrackInfo
import com.example.player.VideoQualityInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val playerManager: MediaPlayerManager = VideoPlayerApplication.instance.playerManager
    private val repository = VideoPlayerApplication.instance.repository
    private val settingsRepository = VideoPlayerApplication.instance.settingsRepository

    val currentVideo: StateFlow<VideoItem?> = playerManager.currentVideo
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackState: StateFlow<Int> = playerManager.playbackState
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val bufferedPosition: StateFlow<Long> = playerManager.bufferedPosition
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val resizeMode: StateFlow<Int> = playerManager.resizeMode
    val isMuted: StateFlow<Boolean> = playerManager.isMuted
    val isLocked: StateFlow<Boolean> = playerManager.isLocked
    val repeatMode: StateFlow<Int> = playerManager.repeatMode
    val isShuffleOn: StateFlow<Boolean> = playerManager.isShuffleOn
    val errorMessage: StateFlow<String?> = playerManager.errorMessage

    val audioTracks: StateFlow<List<AudioTrackInfo>> = playerManager.audioTracks
    val subtitleTracks: StateFlow<List<SubtitleTrackInfo>> = playerManager.subtitleTracks
    val videoQualities: StateFlow<List<VideoQualityInfo>> = playerManager.videoQualities
    val subtitlesEnabled: StateFlow<Boolean> = playerManager.subtitlesEnabled

    val settings: StateFlow<PlayerSettings> = settingsRepository.settings

    // Controls visibility
    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    private var autoHideJob: Job? = null

    // Gesture feedback HUD
    private val _gestureBrightness = MutableStateFlow<Float?>(null)
    val gestureBrightness: StateFlow<Float?> = _gestureBrightness.asStateFlow()

    private val _gestureVolume = MutableStateFlow<Float?>(null)
    val gestureVolume: StateFlow<Float?> = _gestureVolume.asStateFlow()

    private val _gestureSeekFeedback = MutableStateFlow<String?>(null)
    val gestureSeekFeedback: StateFlow<String?> = _gestureSeekFeedback.asStateFlow()

    private val _isSpeedBoosting = MutableStateFlow(false)
    val isSpeedBoosting: StateFlow<Boolean> = _isSpeedBoosting.asStateFlow()

    private var previousSpeedBeforeBoost = 1.0f

    // Dialog state for resuming playback
    private val _resumeDialogVideo = MutableStateFlow<VideoItem?>(null)
    val resumeDialogVideo: StateFlow<VideoItem?> = _resumeDialogVideo.asStateFlow()

    // Track selection dialog states
    private val _showAudioTrackDialog = MutableStateFlow(false)
    val showAudioTrackDialog: StateFlow<Boolean> = _showAudioTrackDialog.asStateFlow()

    private val _showSubtitleTrackDialog = MutableStateFlow(false)
    val showSubtitleTrackDialog: StateFlow<Boolean> = _showSubtitleTrackDialog.asStateFlow()

    private val _showVideoQualityDialog = MutableStateFlow(false)
    val showVideoQualityDialog: StateFlow<Boolean> = _showVideoQualityDialog.asStateFlow()

    private val _showSpeedDialog = MutableStateFlow(false)
    val showSpeedDialog: StateFlow<Boolean> = _showSpeedDialog.asStateFlow()

    init {
        resetAutoHideTimer()
    }

    fun requestPlay(video: VideoItem, forceResume: Boolean? = null) {
        val hasResumePoint = video.lastPosition > 3000L && (video.duration == 0L || video.lastPosition < video.duration * 0.95)

        if (forceResume == true || !hasResumePoint || !settings.value.rememberPlaybackPosition) {
            val startPos = if (forceResume == true) video.lastPosition else 0L
            playerManager.playVideo(video, startPos)
            _resumeDialogVideo.value = null
        } else if (forceResume == false) {
            playerManager.playVideo(video, 0L)
            _resumeDialogVideo.value = null
        } else {
            // Ask user to resume or restart
            _resumeDialogVideo.value = video
        }
        showControls()
    }

    fun playQueue(videos: List<VideoItem>, startIndex: Int = 0) {
        playerManager.playQueue(videos, startIndex)
        showControls()
    }

    fun playUrl(url: String, title: String? = null) {
        viewModelScope.launch {
            repository.recordPlayedUrl(url, title)
            val video = repository.getVideoByUriSync(url) ?: VideoItem(
                uri = url,
                title = title ?: url,
                sourceType = VideoItem.SOURCE_URL
            )
            playerManager.playVideo(video, 0L)
            showControls()
        }
    }

    fun dismissResumeDialog() {
        _resumeDialogVideo.value = null
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
        resetAutoHideTimer()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
        resetAutoHideTimer()
    }

    fun seekBy(seconds: Int) {
        playerManager.seekBy(seconds)
        showSeekFeedback(if (seconds >= 0) "+$seconds" else "$seconds")
        resetAutoHideTimer()
    }

    fun skipNext() {
        playerManager.skipNext()
        resetAutoHideTimer()
    }

    fun skipPrevious() {
        playerManager.skipPrevious()
        resetAutoHideTimer()
    }

    fun setSpeed(speed: Float) {
        playerManager.setSpeed(speed)
        _showSpeedDialog.value = false
        resetAutoHideTimer()
    }

    fun toggleMute() {
        playerManager.toggleMute()
        resetAutoHideTimer()
    }

    fun toggleLock() {
        playerManager.toggleLock()
        if (isLocked.value) {
            _controlsVisible.value = false
        } else {
            showControls()
        }
    }

    fun cycleResizeMode() {
        playerManager.cycleResizeMode()
        resetAutoHideTimer()
    }

    fun cycleRepeatMode() {
        playerManager.cycleRepeatMode()
        resetAutoHideTimer()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
        resetAutoHideTimer()
    }

    fun retry() {
        playerManager.retryPlayback()
        showControls()
    }

    // Subtitles & Audio
    fun openAudioTrackDialog() {
        _showAudioTrackDialog.value = true
    }

    fun closeAudioTrackDialog() {
        _showAudioTrackDialog.value = false
    }

    fun selectAudioTrack(track: AudioTrackInfo?) {
        playerManager.selectAudioTrack(track)
        closeAudioTrackDialog()
    }

    fun openSubtitleTrackDialog() {
        _showSubtitleTrackDialog.value = true
    }

    fun closeSubtitleTrackDialog() {
        _showSubtitleTrackDialog.value = false
    }

    fun selectSubtitleTrack(track: SubtitleTrackInfo?) {
        playerManager.selectSubtitleTrack(track)
        closeSubtitleTrackDialog()
    }

    fun toggleSubtitles() {
        playerManager.toggleSubtitles()
    }

    fun openVideoQualityDialog() {
        _showVideoQualityDialog.value = true
    }

    fun closeVideoQualityDialog() {
        _showVideoQualityDialog.value = false
    }

    fun selectVideoQuality(quality: VideoQualityInfo?) {
        playerManager.selectVideoQuality(quality)
        closeVideoQualityDialog()
    }

    fun openSpeedDialog() {
        _showSpeedDialog.value = true
    }

    fun closeSpeedDialog() {
        _showSpeedDialog.value = false
    }

    // Screen tap
    fun onScreenTap() {
        if (isLocked.value) {
            // In locked mode, briefly show unlock affordance
            _controlsVisible.value = true
            resetAutoHideTimer()
            return
        }
        if (_controlsVisible.value) {
            _controlsVisible.value = false
            autoHideJob?.cancel()
        } else {
            showControls()
        }
    }

    fun showControls() {
        _controlsVisible.value = true
        resetAutoHideTimer()
    }

    fun resetAutoHideTimer() {
        autoHideJob?.cancel()
        val hideDuration = settings.value.autoHideControlsSeconds.coerceIn(2, 10)
        autoHideJob = viewModelScope.launch {
            delay(hideDuration * 1000L)
            if (playerManager.isPlaying.value && !isLocked.value) {
                _controlsVisible.value = false
            }
        }
    }

    // Gestures
    fun onDoubleTap(isRightSide: Boolean) {
        if (isLocked.value || !settings.value.seekGestureEnabled) return
        val step = settings.value.seekDurationSeconds
        if (isRightSide) {
            playerManager.seekBy(step)
            showSeekFeedback("↷ +${step}s")
        } else {
            playerManager.seekBy(-step)
            showSeekFeedback("↶ -${step}s")
        }
        resetAutoHideTimer()
    }

    private fun showSeekFeedback(text: String) {
        _gestureSeekFeedback.value = text
        viewModelScope.launch {
            delay(900)
            if (_gestureSeekFeedback.value == text) {
                _gestureSeekFeedback.value = null
            }
        }
    }

    fun onBrightnessGesture(activity: Activity?, deltaY: Float) {
        if (isLocked.value || !settings.value.brightnessGestureEnabled || activity == null) return
        val window = activity.window
        val layoutParams = window.attributes
        val current = if (layoutParams.screenBrightness < 0f) 0.5f else layoutParams.screenBrightness
        val newBrightness = (current - deltaY * 0.005f).coerceIn(0.01f, 1f)
        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams
        _gestureBrightness.value = newBrightness
        viewModelScope.launch {
            delay(1200)
            _gestureBrightness.value = null
        }
    }

    fun onVolumeGesture(context: Context, deltaY: Float) {
        if (isLocked.value || !settings.value.volumeGestureEnabled) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val deltaVol = (-deltaY * 0.05f).toInt()
        if (deltaVol != 0) {
            val target = (currentVol + deltaVol).coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            val percent = target.toFloat() / maxVol.toFloat()
            _gestureVolume.value = percent
            viewModelScope.launch {
                delay(1200)
                _gestureVolume.value = null
            }
        }
    }

    fun onLongPressStart() {
        if (isLocked.value) return
        if (!_isSpeedBoosting.value) {
            previousSpeedBeforeBoost = playerManager.playbackSpeed.value
            _isSpeedBoosting.value = true
            playerManager.setSpeed(2.0f)
        }
    }

    fun onLongPressEnd() {
        if (_isSpeedBoosting.value) {
            _isSpeedBoosting.value = false
            playerManager.setSpeed(previousSpeedBeforeBoost)
        }
    }
}
