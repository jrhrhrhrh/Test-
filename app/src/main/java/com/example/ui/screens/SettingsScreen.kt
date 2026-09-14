package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by mainViewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PLAYBACK SECTION
            item {
                SettingsSectionCard(
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Playback"
                ) {
                    SettingSwitchRow(
                        title = "Remember Playback Position",
                        subtitle = "Resume from where you stopped watching",
                        checked = settings.rememberPlaybackPosition,
                        onCheckedChange = { mainViewModel.updateSettings(settings.copy(rememberPlaybackPosition = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingSwitchRow(
                        title = "Auto-Play Next Video",
                        subtitle = "Play next item in queue or playlist",
                        checked = settings.autoPlayNext,
                        onCheckedChange = { mainViewModel.updateSettings(settings.copy(autoPlayNext = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingSwitchRow(
                        title = "Background Audio Playback",
                        subtitle = "Keep audio playing when app is minimized",
                        checked = settings.backgroundPlayback,
                        onCheckedChange = { mainViewModel.updateSettings(settings.copy(backgroundPlayback = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Seek Interval: ${settings.seekDurationSeconds} seconds",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Slider(
                            value = settings.seekDurationSeconds.toFloat(),
                            onValueChange = { mainViewModel.updateSettings(settings.copy(seekDurationSeconds = it.toInt())) },
                            valueRange = 5f..30f,
                            steps = 4
                        )
                    }

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Controls Auto-Hide: ${settings.autoHideControlsSeconds} seconds",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Slider(
                            value = settings.autoHideControlsSeconds.toFloat(),
                            onValueChange = { mainViewModel.updateSettings(settings.copy(autoHideControlsSeconds = it.toInt())) },
                            valueRange = 2f..8f,
                            steps = 2
                        )
                    }
                }
            }

            // GESTURES SECTION
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Gesture,
                    title = "Touch & Gestures"
                ) {
                    SettingSwitchRow(
                        title = "Enable Player Gestures",
                        subtitle = "Allow swipe and double-tap gestures on screen",
                        checked = settings.gesturesEnabled,
                        onCheckedChange = { mainViewModel.updateSettings(settings.copy(gesturesEnabled = it)) }
                    )

                    if (settings.gesturesEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingSwitchRow(
                            title = "Brightness Gesture",
                            subtitle = "Swipe vertically on left side to adjust brightness",
                            checked = settings.brightnessGestureEnabled,
                            onCheckedChange = { mainViewModel.updateSettings(settings.copy(brightnessGestureEnabled = it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingSwitchRow(
                            title = "Volume Gesture",
                            subtitle = "Swipe vertically on right side to adjust volume",
                            checked = settings.volumeGestureEnabled,
                            onCheckedChange = { mainViewModel.updateSettings(settings.copy(volumeGestureEnabled = it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingSwitchRow(
                            title = "Double-Tap Seek",
                            subtitle = "Double tap left/right to rewind or fast-forward",
                            checked = settings.seekGestureEnabled,
                            onCheckedChange = { mainViewModel.updateSettings(settings.copy(seekGestureEnabled = it)) }
                        )
                    }
                }
            }

            // SUBTITLES SECTION
            item {
                SettingsSectionCard(
                    icon = Icons.Default.ClosedCaption,
                    title = "Subtitles & Audio"
                ) {
                    SettingSwitchRow(
                        title = "Subtitle Background",
                        subtitle = "Render dark background behind subtitles for readability",
                        checked = settings.subtitleBackground,
                        onCheckedChange = { mainViewModel.updateSettings(settings.copy(subtitleBackground = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Subtitle Size: ${settings.subtitleSizeSp.toInt()} sp",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Slider(
                            value = settings.subtitleSizeSp,
                            onValueChange = { mainViewModel.updateSettings(settings.copy(subtitleSizeSp = it)) },
                            valueRange = 12f..24f,
                            steps = 3
                        )
                    }
                }
            }

            // ABOUT SECTION
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Info,
                    title = "About Application"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "VLC-Inspired Multimedia Engine",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Powered by Android Media3, ExoPlayer, Jetpack Compose, and Room Database. Designed for high performance, smooth gesture controls, adaptive streams (HLS/DASH), and offline local video playback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
