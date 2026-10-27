
package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
internal fun PlayerVideoArea(
    player: ExoPlayer,
    isLandscape: Boolean,
    headphoneModeEnabled: Boolean,
    onToggleHeadphoneMode: () -> Unit,
    orientationLocked: Boolean,
    onToggleOrientationLock: () -> Unit,
    onToggleLandscape: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                IconButton(
                    onClick = onToggleHeadphoneMode
                ) {
                    // CHANGE ICON SIZE HERE: replace 24.dp with your preferred size.
                    HeadphoneIcon(
                        modifier = Modifier.size(24.dp),
                        color = if (headphoneModeEnabled) {
                            PlayerMint
                        } else {
                            Color.White
                        }
                    )
                }
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                IconButton(
                    onClick = onToggleOrientationLock
                ) {
                    // CHANGE ICON SIZE HERE: replace 24.dp with your preferred size.
                    OrientationLockIcon(
                        modifier = Modifier.size(24.dp),
                        locked = orientationLocked,
                        color = if (orientationLocked) {
                            PlayerMint
                        } else {
                            Color.White
                        }
                    )
                }
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                IconButton(onClick = onToggleLandscape) {
                    LandscapeModeIcon(
                        modifier = Modifier.size(24.dp),
                        color = if (isLandscape) PlayerMint else Color.White
                    )
                }
            }
        }
    }
}