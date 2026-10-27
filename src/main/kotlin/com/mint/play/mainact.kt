package com.mint.play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    private lateinit var mediaPermissions: MediaPermissions

    private var audioGranted by mutableStateOf(false)
    private var videoGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the launcher before setContent and before STARTED.
        mediaPermissions = MediaPermissions(this) { audio, video ->
            audioGranted = audio
            videoGranted = video
        }

        audioGranted = mediaPermissions.hasAudioAccess()
        videoGranted = mediaPermissions.hasVideoAccess()

        setContent {
            MaterialTheme {
                MyScreen(
                    audioGranted = audioGranted,
                    videoGranted = videoGranted,
                    onRequestPermissions = {
                        mediaPermissions.requestIfNeeded()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::mediaPermissions.isInitialized) {
            audioGranted = mediaPermissions.hasAudioAccess()
            videoGranted = mediaPermissions.hasVideoAccess()
        }
    }
}

@Composable
fun MyScreen(
    audioGranted: Boolean = false,
    videoGranted: Boolean = false,
    onRequestPermissions: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // The launcher was registered by MainActivity, not by Compose.
    LaunchedEffect(Unit) {
        onRequestPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        TopBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HomeScreen(
                selectedTab = selectedTab,
                audioGranted = audioGranted,
                videoGranted = videoGranted,
                onRequestPermissions = onRequestPermissions
            )
        }

        BottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyScreenPreview() {
    MaterialTheme {
        MyScreen()
    }
}