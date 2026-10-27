
package com.mint.play

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    activity: Activity?,
    modifier: Modifier = Modifier
) {
    var headphoneModeEnabled by remember {
        mutableStateOf(false)
    }

    var orientationLocked by remember {
        mutableStateOf(false)
    }

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
                    onClick = {
                        headphoneModeEnabled = !headphoneModeEnabled
                    }
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
                    onClick = {
                        orientationLocked = !orientationLocked

                        activity?.requestedOrientation =
                            if (orientationLocked) {
                                ActivityInfo.SCREEN_ORIENTATION_LOCKED
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                    }
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
        }
    }
}