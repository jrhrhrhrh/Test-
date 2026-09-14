package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.model.PlayerSettings
import com.example.data.model.VideoItem
import com.example.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
class MediaPlayerManager(private val context: Context) : Player.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    val trackSelector = DefaultTrackSelector(context)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handle audio focus
        )
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
            addListener(this@MediaPlayerManager)
        }

    // Playback States
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<List<VideoItem>>(emptyList())
    val currentPlaylist: StateFlow<List<VideoItem>> = _currentPlaylist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _resizeMode = MutableStateFlow(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffleOn = MutableStateFlow(false)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Track selections
    private val _audioTracks = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrackInfo>> = _subtitleTracks.asStateFlow()

    private val _videoQualities = MutableStateFlow<List<VideoQualityInfo>>(emptyList())
    val videoQualities: StateFlow<List<VideoQualityInfo>> = _videoQualities.asStateFlow()

    private val _subtitlesEnabled = MutableStateFlow(true)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    // Save progress callback
    var onProgressUpdateListener: ((uri: String, position: Long, duration: Long) -> Unit)? = null

    init {
        startProgressTracking()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val buf = player.bufferedPosition.coerceAtLeast(0L)

                    _currentPosition.value = pos
                    _duration.value = dur
                    _bufferedPosition.value = buf

                    _currentVideo.value?.let { video ->
                        if (dur > 0 && pos > 0) {
                            onProgressUpdateListener?.invoke(video.uri, pos, dur)
                        }
                    }
                }
                delay(400)
            }
        }
    }

    fun applySettings(settings: PlayerSettings) {
        setSpeed(settings.defaultSpeed)
        setResizeMode(
            when (settings.resizeMode) {
                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )
    }

    fun playVideo(video: VideoItem, startPositionMs: Long = 0L) {
        playQueue(listOf(video), 0, startPositionMs)
    }

    fun playQueue(videos: List<VideoItem>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (videos.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, videos.size - 1)
        _currentPlaylist.value = videos
        _currentIndex.value = validIndex
        _errorMessage.value = null

        val mediaItems = videos.map { item ->
            val builder = MediaItem.Builder()
                .setUri(Uri.parse(item.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setDisplayTitle(item.title)
                        .build()
                )

            // If HLS, DASH, or specific format, ExoPlayer handles it smoothly
            if (item.uri.endsWith(".m3u8", ignoreCase = true)) {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            } else if (item.uri.endsWith(".mpd", ignoreCase = true)) {
                builder.setMimeType(MimeTypes.APPLICATION_MPD)
            }
            builder.build()
        }

        player.setMediaItems(mediaItems, validIndex, startPositionMs.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = true
        _currentVideo.value = videos[validIndex]

        startPlaybackService()
    }

    private fun startPlaybackService() {
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (_: Exception) {
            // Service startup handled gracefully
        }
    }

    fun play() {
        _errorMessage.value = null
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, duration.value.coerceAtLeast(0L))
        player.seekTo(target)
        _currentPosition.value = target
    }

    fun seekBy(deltaSeconds: Int) {
        val current = player.currentPosition
        val target = (current + deltaSeconds * 1000L).coerceIn(0L, duration.value.coerceAtLeast(0L))
        player.seekTo(target)
        _currentPosition.value = target
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun setSpeed(speed: Float) {
        val validatedSpeed = speed.coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = validatedSpeed
        player.playbackParameters = PlaybackParameters(validatedSpeed)
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        player.volume = if (newMuted) 0f else 1f
    }

    fun setVolume(volumeFactor: Float) {
        val clamped = volumeFactor.coerceIn(0f, 1f)
        player.volume = clamped
        _isMuted.value = (clamped == 0f)
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    fun setLock(locked: Boolean) {
        _isLocked.value = locked
    }

    fun cycleResizeMode() {
        val nextMode = when (_resizeMode.value) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        _resizeMode.value = nextMode
    }

    fun setResizeMode(mode: Int) {
        _resizeMode.value = mode
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = next
        player.repeatMode = next
    }

    fun toggleShuffle() {
        val next = !_isShuffleOn.value
        _isShuffleOn.value = next
        player.shuffleModeEnabled = next
    }

    fun retryPlayback() {
        _errorMessage.value = null
        val curPos = _currentPosition.value
        player.prepare()
        if (curPos > 0) player.seekTo(curPos)
        player.play()
    }

    // Audio Track Selection
    fun selectAudioTrack(trackInfo: AudioTrackInfo?) {
        val parametersBuilder = trackSelector.buildUponParameters()
        if (trackInfo == null) {
            // Auto / Default
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        } else {
            val tracks = player.currentTracks
            val group = tracks.groups[trackInfo.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackInfo.trackIndex))
            parametersBuilder.setOverrideForType(override)
        }
        trackSelector.setParameters(parametersBuilder)
        refreshTracks(player.currentTracks)
    }

    // Subtitle Track Selection
    fun selectSubtitleTrack(trackInfo: SubtitleTrackInfo?) {
        val parametersBuilder = trackSelector.buildUponParameters()
        if (trackInfo == null) {
            // Disabled
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            _subtitlesEnabled.value = false
        } else {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            val tracks = player.currentTracks
            val group = tracks.groups[trackInfo.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackInfo.trackIndex))
            parametersBuilder.setOverrideForType(override)
            _subtitlesEnabled.value = true
        }
        trackSelector.setParameters(parametersBuilder)
        refreshTracks(player.currentTracks)
    }

    fun toggleSubtitles() {
        val willEnable = !_subtitlesEnabled.value
        _subtitlesEnabled.value = willEnable
        val parametersBuilder = trackSelector.buildUponParameters()
        parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !willEnable)
        trackSelector.setParameters(parametersBuilder)
    }

    // Video Quality Selection
    fun selectVideoQuality(qualityInfo: VideoQualityInfo?) {
        val parametersBuilder = trackSelector.buildUponParameters()
        if (qualityInfo == null) {
            // Auto
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            val tracks = player.currentTracks
            val group = tracks.groups[qualityInfo.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(qualityInfo.trackIndex))
            parametersBuilder.setOverrideForType(override)
        }
        trackSelector.setParameters(parametersBuilder)
        refreshTracks(player.currentTracks)
    }

    private fun refreshTracks(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrackInfo>()
        val subtitleList = mutableListOf<SubtitleTrackInfo>()
        val videoList = mutableListOf<VideoQualityInfo>()

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            val trackType = group.type

            for (trackIndex in 0 until group.length) {
                val isSelected = group.isTrackSelected(trackIndex)
                val format = group.getTrackFormat(trackIndex)

                when (trackType) {
                    C.TRACK_TYPE_AUDIO -> {
                        val label = format.label ?: format.language ?: "Audio Track ${audioList.size + 1}"
                        audioList.add(
                            AudioTrackInfo(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                id = format.id ?: "$groupIndex-$trackIndex",
                                label = label,
                                language = format.language ?: "und",
                                sampleRate = format.sampleRate,
                                channelCount = format.channelCount,
                                isSelected = isSelected
                            )
                        )
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val label = format.label ?: format.language ?: "Subtitle ${subtitleList.size + 1}"
                        subtitleList.add(
                            SubtitleTrackInfo(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                id = format.id ?: "$groupIndex-$trackIndex",
                                label = label,
                                language = format.language ?: "und",
                                isSelected = isSelected
                            )
                        )
                    }
                    C.TRACK_TYPE_VIDEO -> {
                        val height = format.height
                        val width = format.width
                        if (height > 0) {
                            val qualityLabel = when {
                                height >= 2160 -> "4K ($height p)"
                                height >= 1440 -> "1440p"
                                height >= 1080 -> "1080p (FHD)"
                                height >= 720 -> "720p (HD)"
                                height >= 480 -> "480p (SD)"
                                height >= 360 -> "360p"
                                else -> "${height}p"
                            }
                            videoList.add(
                                VideoQualityInfo(
                                    groupIndex = groupIndex,
                                    trackIndex = trackIndex,
                                    id = format.id ?: "$groupIndex-$trackIndex",
                                    label = qualityLabel,
                                    width = width,
                                    height = height,
                                    bitrate = format.bitrate,
                                    isSelected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subtitleList
        _videoQualities.value = videoList.sortedByDescending { it.height }
    }

    // Player.Listener Overrides
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    override fun onPlaybackStateChanged(state: Int) {
        _playbackState.value = state
        if (state == Player.STATE_READY) {
            _duration.value = player.duration.coerceAtLeast(0L)
            _errorMessage.value = null
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val index = player.currentMediaItemIndex
        _currentIndex.value = index
        val playlist = _currentPlaylist.value
        if (index in playlist.indices) {
            _currentVideo.value = playlist[index]
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        refreshTracks(tracks)
    }

    override fun onPlayerError(error: PlaybackException) {
        val friendlyMessage = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Network connection error. Check your internet connection and retry."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Video source unavailable or file not found (404/Access Denied)."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED ->
                "Unsupported video or audio format on this device."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                "Media stream format is unsupported or corrupted."
            else -> "Unable to play video: ${error.localizedMessage ?: "Unknown playback error"}"
        }
        _errorMessage.value = friendlyMessage
    }

    fun release() {
        progressJob?.cancel()
        player.removeListener(this)
        player.release()
    }
}
