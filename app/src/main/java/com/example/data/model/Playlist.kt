package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_videos", primaryKeys = ["playlistId", "videoUri"])
data class PlaylistVideoCrossRef(
    val playlistId: Long,
    val videoUri: String,
    val orderIndex: Int = 0
)

data class PlaylistWithVideos(
    val playlist: Playlist,
    val videos: List<VideoItem>
)
