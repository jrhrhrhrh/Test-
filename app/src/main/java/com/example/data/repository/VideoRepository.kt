package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.local.PlaylistDao
import com.example.data.local.RecentUrlDao
import com.example.data.local.VideoDao
import com.example.data.model.Playlist
import com.example.data.model.PlaylistVideoCrossRef
import com.example.data.model.RecentUrl
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao,
    private val recentUrlDao: RecentUrlDao
) {
    val allVideos: Flow<List<VideoItem>> = videoDao.getAllVideos()
    val continueWatching: Flow<List<VideoItem>> = videoDao.getContinueWatching()
    val recentlyPlayed: Flow<List<VideoItem>> = videoDao.getRecentlyPlayed()
    val favorites: Flow<List<VideoItem>> = videoDao.getFavorites()
    val recentUrls: Flow<List<RecentUrl>> = recentUrlDao.getRecentUrls()
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getVideoByUri(uri: String): Flow<VideoItem?> = videoDao.getVideoByUri(uri)

    suspend fun getVideoByUriSync(uri: String): VideoItem? = videoDao.getVideoByUriSync(uri)

    fun searchVideos(query: String): Flow<List<VideoItem>> = videoDao.searchVideos(query)

    suspend fun updateFavorite(uri: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            videoDao.updateFavorite(uri, isFavorite)
        }
    }

    suspend fun updatePlaybackProgress(uri: String, position: Long, duration: Long) {
        withContext(Dispatchers.IO) {
            val existing = videoDao.getVideoByUriSync(uri)
            if (existing == null) {
                // Register it first if opened via direct URL or intent
                val item = VideoItem(
                    uri = uri,
                    title = extractTitleFromUri(uri),
                    duration = duration,
                    lastPlayedTimestamp = System.currentTimeMillis(),
                    lastPosition = position,
                    sourceType = if (uri.startsWith("http")) VideoItem.SOURCE_URL else VideoItem.SOURCE_SAF
                )
                videoDao.insertVideo(item)
            } else {
                videoDao.updatePlaybackProgress(
                    uri = uri,
                    position = position,
                    duration = duration,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    suspend fun deleteVideo(uri: String) {
        withContext(Dispatchers.IO) {
            videoDao.deleteVideo(uri)
        }
    }

    suspend fun recordPlayedUrl(url: String, customTitle: String? = null) {
        withContext(Dispatchers.IO) {
            val title = customTitle?.takeIf { it.isNotBlank() } ?: extractTitleFromUri(url)
            recentUrlDao.insertRecentUrl(RecentUrl(url = url, title = title))
            // Ensure present in videos table
            val existing = videoDao.getVideoByUriSync(url)
            if (existing == null) {
                videoDao.insertVideo(
                    VideoItem(
                        uri = url,
                        title = title,
                        sourceType = VideoItem.SOURCE_URL,
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            } else {
                videoDao.updatePlaybackProgress(
                    uri = url,
                    position = existing.lastPosition,
                    duration = existing.duration,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    suspend fun deleteRecentUrl(url: String) {
        withContext(Dispatchers.IO) {
            recentUrlDao.deleteRecentUrl(url)
        }
    }

    suspend fun clearRecentUrls() {
        withContext(Dispatchers.IO) {
            recentUrlDao.clearAllRecentUrls()
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            playlistDao.insertPlaylist(Playlist(name = name))
        }
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        withContext(Dispatchers.IO) {
            playlistDao.renamePlaylist(playlistId, newName)
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.deletePlaylistVideos(playlistId)
            playlistDao.deletePlaylist(playlistId)
        }
    }

    suspend fun addVideoToPlaylist(playlistId: Long, videoUri: String) {
        withContext(Dispatchers.IO) {
            playlistDao.insertVideoToPlaylist(
                PlaylistVideoCrossRef(
                    playlistId = playlistId,
                    videoUri = videoUri,
                    orderIndex = System.currentTimeMillis().toInt()
                )
            )
        }
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUri: String) {
        withContext(Dispatchers.IO) {
            playlistDao.removeVideoFromPlaylist(playlistId, videoUri)
        }
    }

    fun getPlaylistVideos(playlistId: Long): Flow<List<VideoItem>> =
        playlistDao.getVideosForPlaylist(playlistId)

    suspend fun addVideoFromSafUri(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        try {
            // Persist URI permission so the app can reopen the video later across reboots
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
            // Some providers don't support persistable permissions, continue safely
        }

        val videoItem = queryMetadataForUri(uri)
        videoDao.insertVideo(videoItem)
        videoItem
    }

    private fun queryMetadataForUri(uri: Uri): VideoItem {
        var title = extractTitleFromUri(uri.toString())
        var size = 0L
        var mimeType = "video/*"

        val resolver = context.contentResolver
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) title = name
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
            val resolvedMime = resolver.getType(uri)
            if (!resolvedMime.isNullOrBlank()) {
                mimeType = resolvedMime
            }
        } catch (_: Exception) {
            // Fallback to defaults
        }

        return VideoItem(
            uri = uri.toString(),
            title = title,
            size = size,
            mimeType = mimeType,
            sourceType = VideoItem.SOURCE_SAF
        )
    }

    suspend fun rescanDeviceVideos() = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val videosFound = mutableListOf<VideoItem>()
        try {
            resolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val title = cursor.getString(nameCol) ?: "Video $id"
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val mime = cursor.getString(mimeCol) ?: "video/*"
                    val dateAdded = cursor.getLong(dateCol) * 1000

                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else ""

                    videosFound.add(
                        VideoItem(
                            uri = contentUri.toString(),
                            title = title,
                            duration = duration,
                            size = size,
                            resolution = resolution,
                            mimeType = mime,
                            dateAdded = dateAdded,
                            sourceType = VideoItem.SOURCE_LOCAL
                        )
                    )
                }
            }
            if (videosFound.isNotEmpty()) {
                videoDao.insertVideosIgnore(videosFound)
            }
        } catch (_: Exception) {
            // Handled gracefully if storage permission was denied or media query failed
        }
    }

    private fun extractTitleFromUri(uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            val path = uri.path ?: uriString
            val lastSegment = path.substringAfterLast('/')
            if (lastSegment.isNotBlank()) lastSegment else uriString
        } catch (_: Exception) {
            uriString
        }
    }
}
