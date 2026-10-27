package com.mint.play

import androidx.compose.foundation.background
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
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlayerVideoArea(
            player = player,
            isLandscape = isLandscape,
            headphoneModeEnabled = headphoneModeEnabled,
            onToggleHeadphoneMode = onToggleHeadphoneMode,
            orientationLocked = orientationLocked,
            onToggleOrientationLock = onToggleOrientationLock,
            onToggleLandscape = onToggleLandscape,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f))
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
                .background(PlayerSurface.copy(alpha = 0.92f))
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
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onScrubChange,
                    onValueChangeFinished = onScrubFinished,
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = PlayerMint,
                        activeTrackColor = PlayerMint,
                        inactiveTrackColor = PlayerOutline
                    )
                )
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
                        color = PlayerBackground,
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
                        color = PlayerBackground,
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
