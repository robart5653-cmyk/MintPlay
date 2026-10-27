package com.mint.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    selectedTab: Int,
    audioGranted: Boolean = false,
    videoGranted: Boolean = false,
    onRequestPermissions: () -> Unit = {}
) {
    when (selectedTab) {
        0 -> MediaHomeScreen(
            isVideo = true,
            permissionGranted = videoGranted,
            onRequestPermissions = onRequestPermissions
        )

        1 -> MediaHomeScreen(
            isVideo = false,
            permissionGranted = audioGranted,
            onRequestPermissions = onRequestPermissions
        )

        2 -> PlaceholderScreen(
            title = "Tools",
            message = "Your tools will appear here."
        )

        else -> SettingsTabUI()
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = message,
            color = SecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}