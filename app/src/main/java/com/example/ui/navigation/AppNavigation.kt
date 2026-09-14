package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayUrlScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentVideo by playerViewModel.currentVideo.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    var miniPlayerDismissed by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Playlists,
        Screen.Favorites,
        Screen.Settings
    )

    val isPlayerScreen = currentRoute == Screen.Player.route

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini player bar if video is loaded and not dismissed
                    if (currentVideo != null && !miniPlayerDismissed) {
                        MiniPlayerBar(
                            video = currentVideo!!,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onClick = {
                                navController.navigate(Screen.Player.route)
                            },
                            onTogglePlayPause = {
                                playerViewModel.togglePlayPause()
                            },
                            onClose = {
                                playerViewModel.togglePlayPause()
                                miniPlayerDismissed = true
                            }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(if (isPlayerScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    mainViewModel = mainViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToPlayer = {
                        miniPlayerDismissed = false
                        navController.navigate(Screen.Player.route)
                    },
                    onNavigateToPlayUrl = {
                        navController.navigate(Screen.PlayUrl.route)
                    }
                )
            }

            composable(Screen.PlayUrl.route) {
                PlayUrlScreen(
                    mainViewModel = mainViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToPlayer = {
                        miniPlayerDismissed = false
                        navController.navigate(Screen.Player.route)
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    mainViewModel = mainViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToPlayer = {
                        miniPlayerDismissed = false
                        navController.navigate(Screen.Player.route)
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    mainViewModel = mainViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToPlayer = {
                        miniPlayerDismissed = false
                        navController.navigate(Screen.Player.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    mainViewModel = mainViewModel
                )
            }

            composable(Screen.Player.route) {
                VideoPlayerScreen(
                    playerViewModel = playerViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
