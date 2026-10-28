package com.mint.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset

@Composable
fun AudioPlayerScreen(
    controller: AudioPlayerController,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showQueue by remember { mutableStateOf(false) }
    val song = controller.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("‹", color = PlayerWhite, fontSize = 32.sp)
            }
            Text(
                text = "NOW PLAYING",
                color = PlayerMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { showQueue = true },
                contentPadding = PaddingValues(8.dp)
            ) {
                AudioQueueIcon(
                    modifier = Modifier.size(24.dp),
                    color = PlayerWhite
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = song?.name ?: "No song selected",
                color = PlayerWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.folderName?.ifBlank { "Local audio" } ?: "Choose a song from Music",
                color = PlayerMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AudioSeekBar(controller)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatPlayerTime(controller.position), color = PlayerMuted, fontSize = 12.sp)
            Text(formatPlayerTime(controller.duration), color = PlayerMuted, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = controller::toggleShuffle,
                contentPadding = PaddingValues(8.dp)
            ) {
                AudioShuffleIcon(
                    modifier = Modifier.size(24.dp),
                    color = if (controller.shuffleEnabled) PlayerMint else PlayerMuted
                )
            }
            TextButton(
                onClick = controller::previous,
                contentPadding = PaddingValues(8.dp)
            ) {
                PreviousTrackIcon(
                    modifier = Modifier.size(25.dp),
                    color = PlayerWhite
                )
            }
            TextButton(onClick = controller::togglePlayPause) {
                if (controller.isPlaying) {
                    Text("Ⅱ", color = PlayerMint, fontSize = 28.sp)
                } else {
                    HollowPlayIcon(
                        modifier = Modifier.size(width = 30.dp, height = 32.dp),
                        color = PlayerMint
                    )
                }
            }
            TextButton(
                onClick = controller::next,
                contentPadding = PaddingValues(8.dp)
            ) {
                NextTrackIcon(
                    modifier = Modifier.size(25.dp),
                    color = PlayerWhite
                )
            }
            TextButton(
                onClick = controller::cycleRepeatMode,
                contentPadding = PaddingValues(8.dp)
            ) {
                AudioRepeatIcon(
                    modifier = Modifier.size(24.dp),
                    color = if (controller.repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) PlayerMuted else PlayerMint,
                    repeatOne = controller.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE
                )
            }
        }
    }

    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text("Up next", color = PlayerWhite) },
            text = {
                if (controller.queue.isEmpty()) {
                    Text("The queue is empty.", color = PlayerMuted)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        itemsIndexed(
                            items = controller.queue,
                            key = { _, item -> item.uri.toString() }
                        ) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        controller.playAt(index)
                                        showQueue = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    if (index == controller.currentIndex) "▶" else "♫",
                                    color = if (index == controller.currentIndex) PlayerMint else PlayerMuted
                                )
                                Text(
                                    text = item.name,
                                    color = if (index == controller.currentIndex) PlayerMint else PlayerWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueue = false }) {
                    Text("Close", color = PlayerMint)
                }
            },
            containerColor = NavBackground
        )
    }
}

@Composable
private fun AudioSeekBar(controller: AudioPlayerController) {
    var isScrubbing by remember(controller) { mutableStateOf(false) }
    var scrubProgress by remember(controller) { mutableFloatStateOf(0f) }
    val duration = controller.duration
    val progress = if (isScrubbing) {
        scrubProgress
    } else if (duration > 0L) {
        (controller.position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(duration) {
                detectTapGestures { point ->
                    if (duration > 0L && size.width > 0) {
                        val value = (point.x / size.width.toFloat()).coerceIn(0f, 1f)
                        controller.seekTo((value * duration).toLong())
                    }
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { point ->
                        isScrubbing = true
                        scrubProgress = if (size.width > 0) {
                            (point.x / size.width.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (size.width > 0) {
                            scrubProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        if (duration > 0L) controller.seekTo((scrubProgress * duration).toLong())
                        isScrubbing = false
                    },
                    onDragCancel = { isScrubbing = false }
                )
            }
    ) {
        val centerY = size.height / 2f
        val endX = size.width * progress
        val trackWidth = 2.dp.toPx()
        drawLine(
            color = PlayerOutline,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PlayerMint,
            start = Offset(0f, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = PlayerMint,
            radius = 4.dp.toPx(),
            center = Offset(endX, centerY)
        )
    }
}

@Composable
fun AudioMiniPlayer(
    controller: AudioPlayerController,
    onOpen: () -> Unit
) {
    val song = controller.currentSong ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBackground)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.name,
                color = PlayerWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (controller.isPlaying) "Playing · ${formatPlayerTime(controller.position)}" else "Paused · ${formatPlayerTime(controller.position)}",
                color = PlayerMuted,
                fontSize = 11.sp
            )
        }
        TextButton(onClick = controller::togglePlayPause) {
            if (controller.isPlaying) {
                Text("Ⅱ", color = PlayerMint, fontSize = 22.sp)
            } else {
                HollowPlayIcon(
                    modifier = Modifier.size(width = 22.dp, height = 24.dp),
                    color = PlayerMint
                )
            }
        }
    }
}
