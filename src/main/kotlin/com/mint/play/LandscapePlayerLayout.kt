package com.mint.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@Composable
internal fun LandscapePlayerLayout(
    player: ExoPlayer,
    isLandscape: Boolean,
    onToggleLandscape: () -> Unit,
    headphoneModeEnabled: Boolean,
    onToggleHeadphoneMode: () -> Unit,
    orientationLocked: Boolean,
    onToggleOrientationLock: () -> Unit,
    activeTitle: String,
    onBack: () -> Unit,
    position: Long,
    duration: Long,
    progress: Float,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    isPlaying: Boolean,
    canPrevious: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    canNext: Boolean,
    onNext: () -> Unit,
    speedLabel: String,
    onSpeedClick: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var touchActive by remember { mutableStateOf(false) }

    LaunchedEffect(touchActive) {
        if (!touchActive) {
            delay(5_000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    touchActive = true
                    controlsVisible = true
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                    touchActive = false
                }
            }
    ) {
        PlayerVideoArea(
            player = player,
            isLandscape = isLandscape,
            headphoneModeEnabled = headphoneModeEnabled,
            onToggleHeadphoneMode = onToggleHeadphoneMode,
            orientationLocked = orientationLocked,
            onToggleOrientationLock = onToggleOrientationLock,
            onToggleLandscape = onToggleLandscape,
            showOverlayControls = controlsVisible,
            transparentControlBackground = true,
            modifier = Modifier.fillMaxSize()
        )

        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("‹", color = PlayerWhite, fontSize = 32.sp)
                }
                Text(
                    text = activeTitle,
                    color = PlayerWhite,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlayerTime(position),
                        color = PlayerMuted,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(horizontal = 8.dp)
                            .pointerInput(duration) {
                                detectTapGestures { tap ->
                                    if (duration > 0L && size.width > 0) {
                                        onScrubChange((tap.x / size.width).coerceIn(0f, 1f))
                                        onScrubFinished()
                                    }
                                }
                            }
                            .pointerInput(duration) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (size.width > 0) onScrubChange((offset.x / size.width).coerceIn(0f, 1f))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (size.width > 0) onScrubChange((change.position.x / size.width).coerceIn(0f, 1f))
                                    },
                                    onDragEnd = onScrubFinished,
                                    onDragCancel = onScrubFinished
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            val trackHeight = 2.dp.toPx()
                            val centerY = size.height / 2f
                            val endX = size.width * progress.coerceIn(0f, 1f)

                            drawLine(
                                color = PlayerOutline,
                                start = Offset(0f, centerY),
                                end = Offset(size.width, centerY),
                                strokeWidth = trackHeight
                            )
                            drawLine(
                                color = PlayerMint,
                                start = Offset(0f, centerY),
                                end = Offset(endX, centerY),
                                strokeWidth = trackHeight
                            )
                            drawLine(
                                color = PlayerWhite,
                                start = Offset(endX, centerY - 5.dp.toPx()),
                                end = Offset(endX, centerY + 5.dp.toPx()),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onPrevious, enabled = canPrevious) {
                            Text(
                                text = "|◀",
                                color = if (canPrevious) PlayerWhite else PlayerMuted,
                                fontSize = 19.sp
                            )
                        }
                        TextButton(onClick = onTogglePlayPause) {
                            if (isPlaying) {
                                Text("Ⅱ", color = PlayerMint, fontSize = 26.sp)
                            } else {
                                HollowPlayIcon(
                                    modifier = Modifier.size(width = 28.dp, height = 30.dp),
                                    color = PlayerMint
                                )
                            }
                        }
                        TextButton(onClick = onNext, enabled = canNext) {
                            Text(
                                text = "▶︎|",
                                color = if (canNext) PlayerWhite else PlayerMuted,
                                fontSize = 19.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { /* Subtitle track selection is not connected yet. */ },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            contentColor = PlayerWhite,
                            border = BorderStroke(1.dp, PlayerOutline)
                        ) {
                            Text(
                                text = "CC",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            onClick = onSpeedClick,
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            contentColor = PlayerMint,
                            border = BorderStroke(1.dp, PlayerOutline)
                        ) {
                            Text(
                                text = speedLabel,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
