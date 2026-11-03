package com.mint.play

import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(
    selectedTab: Int,
    audioGranted: Boolean = false,
    videoGranted: Boolean = false,
    onRequestPermissions: () -> Unit = {},
    onVideoPlayerVisibilityChanged: (Boolean) -> Unit = {},
    audioPlayerController: AudioPlayerController,
    showAudioPlayer: Boolean,
    onAudioPlayerVisibilityChanged: (Boolean) -> Unit,
    onAudioSelected: (List<MediaItem>, Int) -> Unit
) {
    when (selectedTab) {
        0 -> MediaHomeScreen(
            isVideo = true,
            permissionGranted = videoGranted,
            onRequestPermissions = onRequestPermissions,
            onVideoPlayerVisibilityChanged = onVideoPlayerVisibilityChanged,
            audioPlayerController = audioPlayerController,
            showAudioPlayer = showAudioPlayer,
            onAudioPlayerVisibilityChanged = onAudioPlayerVisibilityChanged,
            onAudioSelected = onAudioSelected
        )
        1 -> MediaHomeScreen(
            isVideo = false,
            permissionGranted = audioGranted,
            onRequestPermissions = onRequestPermissions,
            onVideoPlayerVisibilityChanged = onVideoPlayerVisibilityChanged,
            audioPlayerController = audioPlayerController,
            showAudioPlayer = showAudioPlayer,
            onAudioPlayerVisibilityChanged = onAudioPlayerVisibilityChanged,
            onAudioSelected = onAudioSelected
        )
        2 -> ToolsScreen()
        else -> SettingsTabUI()
    }
}
