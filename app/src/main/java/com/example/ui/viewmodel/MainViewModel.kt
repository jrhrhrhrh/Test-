package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.VideoPlayerApplication
import com.example.data.model.PlayerSettings
import com.example.data.model.Playlist
import com.example.data.model.RecentUrl
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = VideoPlayerApplication.instance.repository
    private val settingsRepository = VideoPlayerApplication.instance.settingsRepository

    val allVideos: StateFlow<List<VideoItem>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<VideoItem>> = repository.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<VideoItem>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<VideoItem>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentUrls: StateFlow<List<RecentUrl>> = repository.recentUrls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<PlayerSettings> = settingsRepository.settings

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<VideoItem>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchVideos(query.trim())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Details Modal
    private val _videoForDetails = MutableStateFlow<VideoItem?>(null)
    val videoForDetails: StateFlow<VideoItem?> = _videoForDetails.asStateFlow()

    // Active playlist selection for viewing
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _playlistVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val playlistVideos: StateFlow<List<VideoItem>> = _playlistVideos.asStateFlow()

    // Add to playlist modal state
    private val _videoForAddToPlaylist = MutableStateFlow<VideoItem?>(null)
    val videoForAddToPlaylist: StateFlow<VideoItem?> = _videoForAddToPlaylist.asStateFlow()

    init {
        rescanLibrary()
        seedSampleStreamsIfEmpty()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repository.rescanDeviceVideos()
        }
    }

    fun onSafFileSelected(uri: Uri, onComplete: (VideoItem) -> Unit) {
        viewModelScope.launch {
            val item = repository.addVideoFromSafUri(uri)
            onComplete(item)
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.updateFavorite(video.uri, !video.isFavorite)
        }
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(video.uri)
            if (_videoForDetails.value?.uri == video.uri) {
                _videoForDetails.value = null
            }
        }
    }

    fun showVideoDetails(video: VideoItem) {
        _videoForDetails.value = video
    }

    fun dismissVideoDetails() {
        _videoForDetails.value = null
    }

    fun showAddToPlaylist(video: VideoItem) {
        _videoForAddToPlaylist.value = video
    }

    fun dismissAddToPlaylist() {
        _videoForAddToPlaylist.value = null
    }

    fun createPlaylist(name: String, initialVideo: VideoItem? = null) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name.trim().ifEmpty { "New Playlist" })
            if (initialVideo != null) {
                repository.addVideoToPlaylist(id, initialVideo.uri)
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName.trim())
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
                _playlistVideos.value = emptyList()
            }
        }
    }

    fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, video.uri)
            dismissAddToPlaylist()
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, video.uri)
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getPlaylistVideos(playlist.id).collect {
                    _playlistVideos.value = it
                }
            }
        } else {
            _playlistVideos.value = emptyList()
        }
    }

    fun deleteRecentUrl(url: String) {
        viewModelScope.launch {
            repository.deleteRecentUrl(url)
        }
    }

    fun clearRecentUrls() {
        viewModelScope.launch {
            repository.clearRecentUrls()
        }
    }

    fun updateSettings(newSettings: PlayerSettings) {
        settingsRepository.updateSettings(newSettings)
        VideoPlayerApplication.instance.playerManager.applySettings(newSettings)
    }

    private fun seedSampleStreamsIfEmpty() {
        viewModelScope.launch {
            // Provide curated fast demo streams for immediate test capability
            val sampleStreams = listOf(
                RecentUrl(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    title = "Big Buck Bunny (1080p MP4)"
                ),
                RecentUrl(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    title = "Tears of Steel (Sci-Fi MP4)"
                ),
                RecentUrl(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    title = "Elephants Dream (Classic 3D MP4)"
                ),
                RecentUrl(
                    url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    title = "Tears of Steel (Adaptive HLS Stream)"
                )
            )
            for (sample in sampleStreams) {
                repository.recordPlayedUrl(sample.url, sample.title)
            }
        }
    }
}
