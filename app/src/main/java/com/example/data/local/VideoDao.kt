package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY title ASC")
    fun getAllVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE lastPosition > 2000 AND (duration = 0 OR lastPosition < (duration * 0.95)) ORDER BY lastPlayedTimestamp DESC LIMIT 15")
    fun getContinueWatching(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 25")
    fun getRecentlyPlayed(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    fun getVideoByUri(uri: String): Flow<VideoItem?>

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUriSync(uri: String): VideoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideosIgnore(videos: List<VideoItem>)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE videos SET lastPosition = :position, duration = CASE WHEN :duration > 0 THEN :duration ELSE duration END, lastPlayedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePlaybackProgress(uri: String, position: Long, duration: Long, timestamp: Long)

    @Query("DELETE FROM videos WHERE uri = :uri")
    suspend fun deleteVideo(uri: String)

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchVideos(query: String): Flow<List<VideoItem>>
}
