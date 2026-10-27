
package com.mint.play

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    videoUris: List<Uri>,
    videoTitles: List<String>,
    initialIndex: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val activity = remember(context) {
        context.findActivity()
    }

    val controller = remember(context) {
    VideoPlayerController(context)
}

    var currentIndex by remember(videoUris) {
        mutableIntStateOf(
            initialIndex.coerceIn(
                0,
                videoUris.lastIndex.coerceAtLeast(0)
            )
        )
    }

    val activeUri = videoUris.getOrNull(currentIndex)
    val activeTitle = videoTitles.getOrNull(currentIndex) ?: "Video"

    var position by remember {
        mutableLongStateOf(0L)
    }

    var duration by remember {
        mutableLongStateOf(0L)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var speedIndex by remember {
        mutableIntStateOf(0)
    }

    var scrubPosition by remember {
        mutableFloatStateOf(0f)
    }

    var isScrubbing by remember {
        mutableStateOf(false)
    }

    val speeds = listOf(1f, 1.25f, 1.5f, 2f)

    DisposableEffect(controller, activity) {
        onDispose {
            activity?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            controller.release()
        }
    }

    LaunchedEffect(controller, activeUri) {
        if (activeUri != null) {
            controller.play(activeUri)
        }

        position = 0L
        duration = 0L
        scrubPosition = 0f
        isScrubbing = false
    }

    LaunchedEffect(controller) {
        while (true) {
            val player = controller.player

            if (player != null) {
                if (!isScrubbing) {
                    position = player.currentPosition.coerceAtLeast(0L)
                }

                duration = player.duration.coerceAtLeast(0L)
                isPlaying = player.isPlaying
            }

            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBackground)
    ) {
        // Original top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = "‹",
                    color = PlayerWhite,
                    fontSize = 32.sp
                )
            }

            Text(
                text = activeTitle,
                color = PlayerWhite,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Video and right-side overlay buttons stay in PlayerVideoArea.kt
        PlayerVideoArea(
            player = controller.player,
            activity = activity,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Original bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlayerSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Time labels and seek bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlayerTime(position),
                    color = PlayerMuted,
                    fontSize = 11.sp
                )

                // Original thin seek bar with a larger touch area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .padding(horizontal = 8.dp)
                        .pointerInput(duration) {
                            detectTapGestures { tap ->
                                if (duration > 0L && size.width > 0) {
                                    val fraction =
                                        (tap.x / size.width).coerceIn(0f, 1f)

                                    scrubPosition = fraction

                                    controller.seekTo(
                                        (fraction * duration).toLong()
                                    )

                                    isScrubbing = false
                                }
                            }
                        }
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isScrubbing = true

                                    scrubPosition =
                                        (offset.x / size.width)
                                            .coerceIn(0f, 1f)
                                },
                                onDrag = { change, _ ->
                                    change.consume()

                                    scrubPosition =
                                        (change.position.x / size.width)
                                            .coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    if (duration > 0L) {
                                        controller.seekTo(
                                            (scrubPosition * duration).toLong()
                                        )
                                    }

                                    isScrubbing = false
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val progress = (
                        if (isScrubbing) {
                            scrubPosition
                        } else if (duration > 0L) {
                            position.toFloat() / duration.toFloat()
                        } else {
                            0f
                        }
                    ).coerceIn(0f, 1f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        val trackHeight = 2.dp.toPx()
                        val centerY = size.height / 2f
                        val endX = size.width * progress

                        // Inactive track
                        drawLine(
                            color = PlayerOutline,
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = trackHeight
                        )

                        // Active track
                        drawLine(
                            color = PlayerMint,
                            start = Offset(0f, centerY),
                            end = Offset(endX, centerY),
                            strokeWidth = trackHeight
                        )

                        // Original thin vertical progress marker
                        drawLine(
                            color = PlayerWhite,
                            start = Offset(
                                endX,
                                centerY - 5.dp.toPx()
                            ),
                            end = Offset(
                                endX,
                                centerY + 5.dp.toPx()
                            ),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                Text(
                    text = formatPlayerTime(duration),
                    color = PlayerMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            // Original playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous video
                    TextButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Text(
                            text = "|◀",
                            color = if (currentIndex > 0) {
                                PlayerWhite
                            } else {
                                PlayerMuted
                            },
                            fontSize = 19.2.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    // Play / pause
                    TextButton(
                        onClick = {
                            controller.togglePlayPause()
                        }
                    ) {
                        if (isPlaying) {
                            Text(
                                text = "Ⅱ",
                                color = PlayerMint,
                                fontSize = 28.sp
                            )
                        } else {
                            HollowPlayIcon(
                                modifier = Modifier.size(
                                    width = 30.dp,
                                    height = 32.dp
                                ),
                                color = PlayerMint
                            )
                        }
                    }

                    // Next video
                    TextButton(
                        onClick = {
                            if (currentIndex < videoUris.lastIndex) {
                                currentIndex++
                            }
                        },
                        enabled = currentIndex < videoUris.lastIndex
                    ) {
                        Text(
                            text = "▶︎|",
                            color = if (currentIndex < videoUris.lastIndex) {
                                PlayerWhite
                            } else {
                                PlayerMuted
                            },
                            fontSize = 19.2.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                // Original subtitle and playback-speed buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            // Subtitle selection can be connected to Media3 tracks.
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = PlayerBackground,
                        contentColor = PlayerWhite,
                        border = BorderStroke(
                            1.dp,
                            PlayerOutline
                        )
                    ) {
                        Text(
                            text = "CC",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        onClick = {
                            speedIndex = (speedIndex + 1) % speeds.size
                            controller.setSpeed(speeds[speedIndex])
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = PlayerBackground,
                        contentColor = PlayerMint,
                        border = BorderStroke(
                            1.dp,
                            PlayerOutline
                        )
                    ) {
                        Text(
                            text = "${speeds[speedIndex]}×",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}