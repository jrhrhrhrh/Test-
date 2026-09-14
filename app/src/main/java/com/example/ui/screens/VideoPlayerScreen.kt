package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.VideoPlayerApplication
import com.example.ui.components.AudioTrackBottomSheet
import com.example.ui.components.GestureOverlay
import com.example.ui.components.PlaybackSpeedBottomSheet
import com.example.ui.components.PlayerControlsOverlay
import com.example.ui.components.SubtitleTrackBottomSheet
import com.example.ui.components.VideoQualityBottomSheet
import com.example.ui.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val currentVideo by playerViewModel.currentVideo.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val bufferedPosition by playerViewModel.bufferedPosition.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val resizeMode by playerViewModel.resizeMode.collectAsState()
    val isMuted by playerViewModel.isMuted.collectAsState()
    val isLocked by playerViewModel.isLocked.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val isShuffleOn by playerViewModel.isShuffleOn.collectAsState()
    val errorMessage by playerViewModel.errorMessage.collectAsState()

    val controlsVisible by playerViewModel.controlsVisible.collectAsState()
    val brightnessFeedback by playerViewModel.gestureBrightness.collectAsState()
    val volumeFeedback by playerViewModel.gestureVolume.collectAsState()
    val seekFeedback by playerViewModel.gestureSeekFeedback.collectAsState()
    val isSpeedBoosting by playerViewModel.isSpeedBoosting.collectAsState()

    val audioTracks by playerViewModel.audioTracks.collectAsState()
    val subtitleTracks by playerViewModel.subtitleTracks.collectAsState()
    val videoQualities by playerViewModel.videoQualities.collectAsState()
    val subtitlesEnabled by playerViewModel.subtitlesEnabled.collectAsState()

    val showAudioDialog by playerViewModel.showAudioTrackDialog.collectAsState()
    val showSubtitleDialog by playerViewModel.showSubtitleTrackDialog.collectAsState()
    val showQualityDialog by playerViewModel.showVideoQualityDialog.collectAsState()
    val showSpeedDialog by playerViewModel.showSpeedDialog.collectAsState()

    var isLandscape by remember { mutableStateOf(false) }

    // Keep screen on while playing
    DisposableEffect(activity, isPlaying) {
        if (activity != null) {
            if (isPlaying) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Reset orientation on exit
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val player = VideoPlayerApplication.instance.playerManager.player

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. AndroidView PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Touch Gesture Handler
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val screenWidth = size.width
                        val isRightSide = change.position.x > (screenWidth / 2)
                        if (isRightSide) {
                            playerViewModel.onVolumeGesture(context, dragAmount)
                        } else {
                            playerViewModel.onBrightnessGesture(activity, dragAmount)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { playerViewModel.onScreenTap() },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            val isRightSide = offset.x > (screenWidth / 2)
                            playerViewModel.onDoubleTap(isRightSide)
                        },
                        onLongPress = {
                            playerViewModel.onLongPressStart()
                        },
                        onPress = {
                            val released = tryAwaitRelease()
                            if (released) {
                                playerViewModel.onLongPressEnd()
                            }
                        }
                    )
                }
        )

        // 3. Gesture Feedback HUD (Brightness, Volume, Seek, 2x Boost)
        GestureOverlay(
            brightness = brightnessFeedback,
            volume = volumeFeedback,
            seekFeedback = seekFeedback,
            isSpeedBoosting = isSpeedBoosting
        )

        // 4. Custom Controls Overlay
        PlayerControlsOverlay(
            isVisible = controlsVisible,
            isLocked = isLocked,
            isPlaying = isPlaying,
            isBuffering = playbackState == Player.STATE_BUFFERING,
            currentPosition = currentPosition,
            duration = duration,
            bufferedPosition = bufferedPosition,
            videoTitle = currentVideo?.title ?: "Playing Video",
            playbackSpeed = playbackSpeed,
            resizeMode = resizeMode,
            isMuted = isMuted,
            repeatMode = repeatMode,
            isShuffleOn = isShuffleOn,
            onTogglePlayPause = { playerViewModel.togglePlayPause() },
            onSeekTo = { playerViewModel.seekTo(it) },
            onSeekBy = { playerViewModel.seekBy(it) },
            onSkipNext = { playerViewModel.skipNext() },
            onSkipPrevious = { playerViewModel.skipPrevious() },
            onToggleLock = { playerViewModel.toggleLock() },
            onCycleResizeMode = { playerViewModel.cycleResizeMode() },
            onCycleRepeatMode = { playerViewModel.cycleRepeatMode() },
            onToggleShuffle = { playerViewModel.toggleShuffle() },
            onToggleMute = { playerViewModel.toggleMute() },
            onOpenSpeedDialog = { playerViewModel.openSpeedDialog() },
            onOpenAudioDialog = { playerViewModel.openAudioTrackDialog() },
            onOpenSubtitleDialog = { playerViewModel.openSubtitleTrackDialog() },
            onOpenQualityDialog = { playerViewModel.openVideoQualityDialog() },
            onEnterPiP = {
                enterPictureInPicture(activity, player.videoSize.width, player.videoSize.height)
            },
            onToggleOrientation = {
                if (activity != null) {
                    if (isLandscape) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        isLandscape = false
                    } else {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        isLandscape = true
                    }
                }
            },
            onBack = onBack
        )

        // 5. Error Overlay (if stream / format error occurs)
        if (errorMessage != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { playerViewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Retry Playback")
                    }
                }
            }
        }
    }

    // Modal Sheets
    if (showAudioDialog) {
        AudioTrackBottomSheet(
            tracks = audioTracks,
            onSelectTrack = { playerViewModel.selectAudioTrack(it) },
            onDismiss = { playerViewModel.closeAudioTrackDialog() }
        )
    }

    if (showSubtitleDialog) {
        SubtitleTrackBottomSheet(
            tracks = subtitleTracks,
            subtitlesEnabled = subtitlesEnabled,
            onSelectTrack = { playerViewModel.selectSubtitleTrack(it) },
            onDismiss = { playerViewModel.closeSubtitleTrackDialog() }
        )
    }

    if (showQualityDialog) {
        VideoQualityBottomSheet(
            qualities = videoQualities,
            onSelectQuality = { playerViewModel.selectVideoQuality(it) },
            onDismiss = { playerViewModel.closeVideoQualityDialog() }
        )
    }

    if (showSpeedDialog) {
        PlaybackSpeedBottomSheet(
            currentSpeed = playbackSpeed,
            onSelectSpeed = { playerViewModel.setSpeed(it) },
            onDismiss = { playerViewModel.closeSpeedDialog() }
        )
    }
}

private fun enterPictureInPicture(activity: Activity?, width: Int, height: Int) {
    if (activity == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val aspectRational = if (width > 0 && height > 0) {
                val clampedW = width.coerceIn(1, 10000)
                val clampedH = height.coerceIn(1, 10000)
                // PiP aspect ratio must be between 1:2.39 and 2.39:1
                val ratio = clampedW.toFloat() / clampedH.toFloat()
                if (ratio in 0.418f..2.39f) {
                    Rational(clampedW, clampedH)
                } else {
                    Rational(16, 9)
                }
            } else {
                Rational(16, 9)
            }

            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRational)
                .build()
            activity.enterPictureInPictureMode(pipParams)
        } catch (_: Exception) {
            // Handled gracefully on devices that do not support PiP
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
