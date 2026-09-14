package com.example.player

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String,
    val label: String,
    val language: String,
    val sampleRate: Int,
    val channelCount: Int,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class VideoQualityInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String,
    val label: String, // e.g., "1080p", "720p", "480p"
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val isSelected: Boolean
)
