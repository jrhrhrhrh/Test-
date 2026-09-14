package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey
    val uri: String,
    val title: String,
    val duration: Long = 0L,
    val size: Long = 0L,
    val resolution: String = "",
    val mimeType: String = "video/*",
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayedTimestamp: Long = 0L,
    val lastPosition: Long = 0L,
    val isFavorite: Boolean = false,
    val sourceType: String = SOURCE_LOCAL // "LOCAL", "URL", "SAF"
) {
    val isCompleted: Boolean
        get() = duration > 0 && lastPosition >= (duration * 0.95)

    val progressPercent: Float
        get() = if (duration > 0) (lastPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedDuration: String
        get() = formatDurationMs(duration)

    val formattedLastPosition: String
        get() = formatDurationMs(lastPosition)

    val formattedSize: String
        get() {
            if (size <= 0) return ""
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }

    companion object {
        const val SOURCE_LOCAL = "LOCAL"
        const val SOURCE_URL = "URL"
        const val SOURCE_SAF = "SAF"

        fun formatDurationMs(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSeconds = ms / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
