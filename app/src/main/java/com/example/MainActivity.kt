package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.data.model.VideoItem
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.Screen
import com.example.ui.theme.VideoPlayerTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val settings by mainViewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val navController = rememberNavController()

            VideoPlayerTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        navController = navController,
                        mainViewModel = mainViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val dataUri: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && dataUri != null) {
            val uriString = dataUri.toString()
            val isNetwork = uriString.startsWith("http://", ignoreCase = true) ||
                    uriString.startsWith("https://", ignoreCase = true) ||
                    uriString.startsWith("rtsp://", ignoreCase = true)

            if (isNetwork) {
                playerViewModel.playUrl(uriString)
            } else {
                mainViewModel.onSafFileSelected(dataUri) { videoItem ->
                    playerViewModel.requestPlay(videoItem, forceResume = false)
                }
            }
        }
    }
}

