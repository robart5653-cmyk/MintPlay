
package com.mint.play

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

enum class PlayerGestureType {
    Brightness,
    Volume,
    SeekBackward,
    SeekForward
}

data class PlayerGestureFeedback(
    val type: PlayerGestureType,
    val percent: Int,
    val eventId: Int,
    val seekSeconds: Long = 0L
)

internal fun readCurrentBrightnessPercent(context: Context, activity: Activity?): Int {
    val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
    val brightness = if (windowBrightness >= 0f) {
        windowBrightness
    } else {
        try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            ).toFloat() / 255f
        } catch (_: Exception) {
            0.5f
        }
    }
    return (brightness.coerceIn(0f, 1f) * 100f).roundToInt()
}

internal fun readCurrentVolumePercent(audioManager: AudioManager): Int {
    val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (maximum <= 0) return 0
    return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / maximum)
        .roundToInt()
        .coerceIn(0, 100)
}

@Composable
internal fun PlayerVideoArea(
    player: ExoPlayer,
    activity: Activity?,
    seekSeconds: Int,
    gestureFeedback: PlayerGestureFeedback?,
    onBrightnessPercentChange: (Int) -> Unit,
    onVolumePercentChange: (Int) -> Unit,
    onGestureFeedback: (PlayerGestureType, Int) -> Unit,
    onSeekGesture: (Boolean, Int) -> Unit,
    isLandscape: Boolean,
    rotationDegrees: Int,
    headphoneModeEnabled: Boolean,
    onToggleHeadphoneMode: () -> Unit,
    orientationLocked: Boolean,
    onToggleOrientationLock: () -> Unit,
    onToggleLandscape: () -> Unit,
    showOverlayControls: Boolean = true,
    transparentControlBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player, seekSeconds) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            // Left half queues a rewind; right half queues a forward skip.
                            // The parent waits briefly so rapid taps can be combined into one seek.
                            onSeekGesture(
                                tap.x < size.width / 2f,
                                seekSeconds.coerceAtLeast(1)
                            )
                        }
                    )
                }
                .pointerInput(player, activity) {
                    var gestureType = PlayerGestureType.Brightness
                    var startPercent = 0
                    var accumulatedPercent = 0f
                    var lastPercent = -1

                    detectVerticalDragGestures(
                        onDragStart = { start ->
                            gestureType = if (start.x < size.width / 2f) {
                                PlayerGestureType.Brightness
                            } else {
                                PlayerGestureType.Volume
                            }
                            startPercent = if (gestureType == PlayerGestureType.Brightness) {
                                readCurrentBrightnessPercent(context, activity)
                            } else {
                                readCurrentVolumePercent(audioManager)
                            }
                            accumulatedPercent = 0f
                            lastPercent = startPercent
                            onGestureFeedback(gestureType, startPercent)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (size.height > 0) {
                                // A full-height upward swipe changes the value by 100%.
                                accumulatedPercent += -dragAmount / size.height.toFloat() * 100f
                                val nextPercent = (startPercent + accumulatedPercent)
                                    .roundToInt()
                                    .coerceIn(0, 100)

                                if (nextPercent != lastPercent) {
                                    lastPercent = nextPercent
                                    if (gestureType == PlayerGestureType.Brightness) {
                                        onBrightnessPercentChange(nextPercent)
                                        onGestureFeedback(gestureType, nextPercent)
                                    } else {
                                        onVolumePercentChange(nextPercent)
                                        onGestureFeedback(
                                            gestureType,
                                            readCurrentVolumePercent(audioManager)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                // The layout dimensions swap before rotating so the rotated video still
                // fills the available area instead of being clipped by its old bounds.
                .then(
                    if (rotationDegrees == 0) Modifier
                    else Modifier.requiredSize(width = maxHeight, height = maxWidth)
                ),
            factory = { context ->
                PlayerView(context).apply {
                    rotation = rotationDegrees.toFloat()
                    useController = false
                    useArtwork = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
                view.rotation = rotationDegrees.toFloat()
            }
        )

        if (headphoneModeEnabled) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Background play enabled",
                    color = PlayerMint,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (gestureFeedback != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.76f),
                shadowElevation = 4.dp
            ) {
                if (gestureFeedback.type == PlayerGestureType.SeekBackward ||
                    gestureFeedback.type == PlayerGestureType.SeekForward
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (gestureFeedback.type == PlayerGestureType.SeekBackward) "◀◀" else "▶▶",
                            color = PlayerMint,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val sign = if (gestureFeedback.type == PlayerGestureType.SeekBackward) "−" else "+"
                        val seconds = gestureFeedback.seekSeconds.coerceAtLeast(0L)
                        val amountText = if (seconds >= 60L) {
                            "$sign${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                        } else {
                            "$sign${seconds}s"
                        }
                        Text(
                            text = amountText,
                            color = PlayerWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (gestureFeedback.type == PlayerGestureType.Brightness) {
                                    "Brightness"
                                } else {
                                    "Volume"
                                },
                                color = PlayerWhite,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${gestureFeedback.percent.coerceIn(0, 100)}%",
                                color = PlayerMint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .width(156.dp)
                                .height(4.dp)
                        ) {
                            val barSize = Size(
                                size.width * gestureFeedback.percent.coerceIn(0, 100) / 100f,
                                size.height
                            )
                            val radius = CornerRadius(size.height / 2f)
                            drawRoundRect(
                                color = PlayerOutline,
                                size = Size(size.width, size.height),
                                cornerRadius = radius
                            )
                            if (barSize.width > 0f) {
                                drawRoundRect(
                                    color = PlayerMint,
                                    size = barSize,
                                    cornerRadius = radius
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showOverlayControls) {
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
                    color = if (transparentControlBackground) Color.Transparent else Color.Black.copy(alpha = 0.45f)
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
                    color = if (transparentControlBackground) Color.Transparent else Color.Black.copy(alpha = 0.45f)
                ) {
                    IconButton(
                        onClick = onToggleOrientationLock
                    ) {
                        // CHANGE ICON SIZE HERE: replace 24.dp with your preferred size.
                        RotationModeIcon(
                            modifier = Modifier.size(24.dp),
                            rotationDegrees = rotationDegrees,
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
                    color = if (transparentControlBackground) Color.Transparent else Color.Black.copy(alpha = 0.45f)
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
}
