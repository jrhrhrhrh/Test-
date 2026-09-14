package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Playlist
import com.example.data.model.PlaylistVideoCrossRef
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun deletePlaylistVideos(playlistId: Long)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoToPlaylist(crossRef: PlaylistVideoCrossRef)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoUri = :videoUri")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUri: String)

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN playlist_videos pv ON v.uri = pv.videoUri
        WHERE pv.playlistId = :playlistId
        ORDER BY pv.orderIndex ASC
    """)
    fun getVideosForPlaylist(playlistId: Long): Flow<List<VideoItem>>

    @Query("SELECT COUNT(*) FROM playlist_videos WHERE playlistId = :playlistId")
    fun getPlaylistVideoCount(playlistId: Long): Flow<Int>
}
