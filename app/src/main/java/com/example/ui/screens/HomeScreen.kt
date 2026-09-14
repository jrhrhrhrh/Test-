package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoItem
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.EmptyState
import com.example.ui.components.ResumePlaybackDialog
import com.example.ui.components.VideoDetailsDialog
import com.example.ui.components.VideoListCard
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel

enum class VideoSortOption {
    NAME_ASC, DATE_DESC, DURATION_DESC, SIZE_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlayUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allVideos by mainViewModel.allVideos.collectAsState()
    val continueWatching by mainViewModel.continueWatching.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val searchResults by mainViewModel.searchResults.collectAsState()
    val videoForDetails by mainViewModel.videoForDetails.collectAsState()
    val videoForAddToPlaylist by mainViewModel.videoForAddToPlaylist.collectAsState()
    val playlists by mainViewModel.playlists.collectAsState()
    val resumeDialogVideo by playerViewModel.resumeDialogVideo.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(VideoSortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    // System SAF File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            mainViewModel.onSafFileSelected(uri) { item ->
                playerViewModel.requestPlay(item, forceResume = false)
                onNavigateToPlayer()
            }
        }
    }

    val sortedVideos = remember(allVideos, currentSort) {
        when (currentSort) {
            VideoSortOption.NAME_ASC -> allVideos.sortedBy { it.title.lowercase() }
            VideoSortOption.DATE_DESC -> allVideos.sortedByDescending { it.dateAdded }
            VideoSortOption.DURATION_DESC -> allVideos.sortedByDescending { it.duration }
            VideoSortOption.SIZE_DESC -> allVideos.sortedByDescending { it.size }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { mainViewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search your videos...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Video Player",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            mainViewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search videos")
                        }
                        IconButton(onClick = { mainViewModel.rescanLibrary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan library")
                        }
                    }
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // SEARCH RESULTS VIEW
            if (isSearchActive && searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Results for \"$searchQuery\" (${searchResults.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "No matches found",
                            description = "Try searching for a different video title or keyword."
                        )
                    }
                } else {
                    items(searchResults, key = { it.uri }) { video ->
                        VideoListCard(
                            video = video,
                            onClick = {
                                playerViewModel.requestPlay(video)
                                onNavigateToPlayer()
                            },
                            onToggleFavorite = { mainViewModel.toggleFavorite(video) },
                            onShowDetails = { mainViewModel.showVideoDetails(video) },
                            onAddToPlaylist = { mainViewModel.showAddToPlaylist(video) },
                            onDelete = { mainViewModel.deleteVideo(video) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                // NORMAL HOME VIEW
                // 1. Quick Actions Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.Link,
                            title = "Play URL",
                            subtitle = "Stream link",
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToPlayUrl,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            icon = Icons.Default.FileOpen,
                            title = "Open File",
                            subtitle = "Storage pick",
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            iconColor = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                filePickerLauncher.launch(arrayOf("video/*"))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Continue Watching Horizontal Section
                if (continueWatching.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                items(continueWatching, key = { it.uri }) { video ->
                                    ContinueWatchingCard(
                                        video = video,
                                        onClick = {
                                            playerViewModel.requestPlay(video, forceResume = true)
                                            onNavigateToPlayer()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. All Videos Section Header with Sort
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Videos (${sortedVideos.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort videos",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date Added (Newest)") },
                                    onClick = {
                                        currentSort = VideoSortOption.DATE_DESC
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Title (A-Z)") },
                                    onClick = {
                                        currentSort = VideoSortOption.NAME_ASC
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duration (Longest)") },
                                    onClick = {
                                        currentSort = VideoSortOption.DURATION_DESC
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest)") },
                                    onClick = {
                                        currentSort = VideoSortOption.SIZE_DESC
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Video Items or Empty State
                if (sortedVideos.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Videocam,
                            title = "No videos detected yet",
                            description = "Tap 'Open File' to pick a video from your device storage, or 'Play URL' to stream online videos.",
                            actionLabel = "Open File",
                            onActionClick = {
                                filePickerLauncher.launch(arrayOf("video/*"))
                            }
                        )
                    }
                } else {
                    items(sortedVideos, key = { it.uri }) { video ->
                        VideoListCard(
                            video = video,
                            onClick = {
                                playerViewModel.requestPlay(video)
                                onNavigateToPlayer()
                            },
                            onToggleFavorite = { mainViewModel.toggleFavorite(video) },
                            onShowDetails = { mainViewModel.showVideoDetails(video) },
                            onAddToPlaylist = { mainViewModel.showAddToPlaylist(video) },
                            onDelete = { mainViewModel.deleteVideo(video) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    videoForDetails?.let { video ->
        VideoDetailsDialog(
            video = video,
            onDismiss = { mainViewModel.dismissVideoDetails() },
            onPlay = {
                playerViewModel.requestPlay(video)
                onNavigateToPlayer()
            },
            onToggleFavorite = { mainViewModel.toggleFavorite(video) }
        )
    }

    videoForAddToPlaylist?.let { video ->
        AddToPlaylistDialog(
            video = video,
            playlists = playlists,
            onAddToPlaylist = { playlistId ->
                mainViewModel.addVideoToPlaylist(playlistId, video)
            },
            onCreateNewPlaylist = { name ->
                mainViewModel.createPlaylist(name, video)
                mainViewModel.dismissAddToPlaylist()
            },
            onDismiss = { mainViewModel.dismissAddToPlaylist() }
        )
    }

    resumeDialogVideo?.let { video ->
        ResumePlaybackDialog(
            video = video,
            onResume = {
                playerViewModel.requestPlay(video, forceResume = true)
                onNavigateToPlayer()
            },
            onStartOver = {
                playerViewModel.requestPlay(video, forceResume = false)
                onNavigateToPlayer()
            },
            onDismiss = { playerViewModel.dismissResumeDialog() }
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
