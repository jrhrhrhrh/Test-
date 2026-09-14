package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_urls")
data class RecentUrl(
    @PrimaryKey
    val url: String,
    val title: String,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)
