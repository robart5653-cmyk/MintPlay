
package com.mint.play

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    videoUris: List<Uri>,
    videoTitles: List<String>,
    initialIndex: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val activity = remember(context) {
        context.findActivity()
    }
    val originalSystemBarsVisible = remember(activity) {
        activity?.window?.let { window ->
            ViewCompat.getRootWindowInsets(window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.systemBars()) ?: true
        } ?: true
    }
    val originalWindowBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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

    // Keep the display awake only while this video is actively playing.
    DisposableEffect(activity, isPlaying) {
        val window = activity?.window
        if (isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (isPlaying) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    var speedIndex by remember {
        mutableIntStateOf(0)
    }

    var headphoneModeEnabled by remember {
        mutableStateOf(false)
    }

    val toggleHeadphoneMode = {
        val enabled = !headphoneModeEnabled
        controller.setAudioOnlyMode(enabled)
        headphoneModeEnabled = enabled
    }

    // The button cycles through clockwise, counterclockwise, then portrait.
    var rotationLockState by remember {
        mutableIntStateOf(0)
    }
    val orientationLocked = rotationLockState != 0

    var scrubPosition by remember {
        mutableFloatStateOf(0f)
    }

    var isScrubbing by remember {
        mutableStateOf(false)
    }

    val speeds = listOf(1f, 1.25f, 1.5f, 2f)
    val doubleTapSeekSeconds = remember(context) {
        VideoSettingsStore.getDoubleTapSeekSeconds(context)
    }
    var gestureFeedback by remember { mutableStateOf<PlayerGestureFeedback?>(null) }
    var gestureFeedbackEventId by remember { mutableIntStateOf(0) }
    var pendingSeekMs by remember { mutableLongStateOf(0L) }
    var pendingSeekDirection by remember { mutableIntStateOf(0) }
    var pendingSeekEventId by remember { mutableIntStateOf(0) }

    val onBrightnessPercentChange: (Int) -> Unit = { percent ->
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = percent.coerceIn(0, 100) / 100f
            window.attributes = attributes
        }
    }
    val onVolumePercentChange: (Int) -> Unit = { percent ->
        val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maximum > 0) {
            val targetVolume = (maximum * percent.coerceIn(0, 100) / 100f)
                .roundToInt()
                .coerceIn(0, maximum)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }
    val onGestureFeedback: (PlayerGestureType, Int) -> Unit = { type, percent ->
        gestureFeedbackEventId += 1
        gestureFeedback = PlayerGestureFeedback(type, percent, gestureFeedbackEventId)
    }
    val onSeekGesture: (Boolean, Int) -> Unit = { isBackward, seconds ->
        val direction = if (isBackward) -1 else 1
        val incrementMs = seconds.coerceAtLeast(1).toLong() * 1_000L
        // Repeated taps in the same direction add together. Changing direction starts a new total.
        val nextTotalMs = if (pendingSeekMs > 0L && pendingSeekDirection == direction) {
            pendingSeekMs + incrementMs
        } else {
            incrementMs
        }
        pendingSeekDirection = direction
        pendingSeekMs = nextTotalMs
        pendingSeekEventId += 1
        gestureFeedbackEventId += 1
        gestureFeedback = PlayerGestureFeedback(
            type = if (isBackward) PlayerGestureType.SeekBackward else PlayerGestureType.SeekForward,
            percent = 0,
            eventId = gestureFeedbackEventId,
            seekSeconds = nextTotalMs / 1_000L
        )
    }

    val resumePlaybackEnabled = remember(context) {
        VideoSettingsStore.getResumePlayback(context)
    }
    val defaultOrientation = remember(context) {
        VideoSettingsStore.getDefaultOrientation(context)
    }

    val applyDefaultOrientation = {
        activity?.requestedOrientation = when (defaultOrientation) {
            "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val saveCurrentPlaybackPosition: () -> Unit = {
        val player = controller.player
        val currentUri = player.currentMediaItem
            ?.localConfiguration
            ?.uri
            ?.toString()

        if (resumePlaybackEnabled && currentUri != null) {
            val currentPosition = player.currentPosition.coerceAtLeast(0L)
            val videoDuration = player.duration

            // A finished video should start from the beginning next time.
            if (videoDuration > 0L && currentPosition >= videoDuration - 2_000L) {
                VideoSettingsStore.setPlaybackPosition(context, currentUri, 0L)
            } else {
                VideoSettingsStore.setPlaybackPosition(context, currentUri, currentPosition)
            }
        }
    }

    DisposableEffect(controller, activity, originalSystemBarsVisible) {
        onDispose {
            saveCurrentPlaybackPosition()
            activity?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                if (originalSystemBarsVisible) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = originalWindowBrightness
                window.attributes = attributes
            }
            controller.release()
        }
    }

    // Video-only lock handling: pause on screen-off and resume after the user unlocks.
    DisposableEffect(context, controller, headphoneModeEnabled) {
        var resumeAfterUnlock = false

        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        val player = controller.player
                        if (!headphoneModeEnabled && !resumeAfterUnlock && player.isPlaying) {
                            resumeAfterUnlock = true
                            saveCurrentPlaybackPosition()
                            player.pause()
                        }
                    }

                    Intent.ACTION_USER_PRESENT -> {
                        if (resumeAfterUnlock) {
                            // Give Android a moment to restore the app window. If the
                            // user unlocked straight to the launcher, do not resume video
                            // audio while MintPlay is still in the background.
                            activity?.window?.decorView?.postDelayed({
                                if (resumeAfterUnlock) {
                                    resumeAfterUnlock = false
                                    if (activity.hasWindowFocus()) {
                                        controller.player.play()
                                    }
                                }
                            }, 400L) ?: run {
                                resumeAfterUnlock = false
                            }
                        }
                    }
                }
            }
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenReceiver, screenFilter)

        onDispose {
            context.unregisterReceiver(screenReceiver)
        }
    }

    LaunchedEffect(controller, activity) {
        applyDefaultOrientation()
    }

    // Wait until double-taps stop arriving, then apply their combined seek once.
    LaunchedEffect(pendingSeekEventId) {
        if (pendingSeekMs <= 0L) return@LaunchedEffect
        delay(650L)
        val totalMs = pendingSeekMs
        val direction = pendingSeekDirection
        if (totalMs > 0L && direction != 0) {
            val player = controller.player
            val currentPosition = player.currentPosition.coerceAtLeast(0L)
            val durationMs = player.duration
            val target = (currentPosition + direction.toLong() * totalMs).coerceAtLeast(0L)
            player.seekTo(if (durationMs > 0L) target.coerceAtMost(durationMs) else target)
            pendingSeekMs = 0L
            pendingSeekDirection = 0
        }
    }

    LaunchedEffect(gestureFeedback?.eventId) {
        val currentFeedback = gestureFeedback ?: return@LaunchedEffect
        delay(5_000L)
        if (gestureFeedback?.eventId == currentFeedback.eventId) {
            gestureFeedback = null
        }
    }

    LaunchedEffect(isLandscape, activity, originalSystemBarsVisible) {
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (isLandscape) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            if (originalSystemBarsVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = true, onBack = onBack)

    val toggleOrientationLock = {
        when (rotationLockState) {
            0 -> {
                rotationLockState = 1
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            1 -> {
                rotationLockState = 2
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            else -> {
                rotationLockState = 0
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    LaunchedEffect(controller, activeUri) {
        if (!resumePlaybackEnabled) {
            // Turning this setting off means videos always start at the beginning.
            VideoSettingsStore.clearPlaybackPositions(context)
        }

        // Save the previous playlist item before switching to the next one.
        saveCurrentPlaybackPosition()

        if (activeUri != null) {
            MediaHistoryStore.addVideo(context, activeUri)
            controller.play(activeUri)

            if (resumePlaybackEnabled) {
                val savedPosition = VideoSettingsStore.getPlaybackPosition(
                    context,
                    activeUri.toString()
                )
                if (savedPosition > 0L) {
                    controller.seekTo(savedPosition)
                }
            }
        }

        position = 0L
        duration = 0L
        scrubPosition = 0f
        isScrubbing = false
    }

    LaunchedEffect(controller) {
        var lastPositionSaveAt = 0L

        while (true) {
            val player = controller.player

            if (!isScrubbing) {
                position = player.currentPosition.coerceAtLeast(0L)
            }

            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying

            val now = System.currentTimeMillis()
            if (now - lastPositionSaveAt >= 1_000L) {
                saveCurrentPlaybackPosition()
                lastPositionSaveAt = now
            }

            delay(250)
        }
    }

    if (isLandscape) {
        LandscapePlayerLayout(
            player = controller.player,
            isLandscape = true,
            headphoneModeEnabled = headphoneModeEnabled,
            onToggleHeadphoneMode = toggleHeadphoneMode,
            orientationLocked = orientationLocked,
            onToggleOrientationLock = toggleOrientationLock,
            activeTitle = activeTitle,
            onBack = onBack,
            position = position,
            duration = duration,
            progress = (if (isScrubbing) scrubPosition else if (duration > 0L) position.toFloat() / duration.toFloat() else 0f).coerceIn(0f, 1f),
            onScrubChange = {
                scrubPosition = it
                isScrubbing = true
            },
            onScrubFinished = {
                if (duration > 0L) controller.seekTo((scrubPosition * duration).toLong())
                isScrubbing = false
            },
            isPlaying = isPlaying,
            canPrevious = currentIndex > 0,
            onPrevious = { if (currentIndex > 0) currentIndex-- },
            onTogglePlayPause = { controller.togglePlayPause() },
            canNext = currentIndex < videoUris.lastIndex,
            onNext = { if (currentIndex < videoUris.lastIndex) currentIndex++ },
            speedLabel = "${speeds[speedIndex]}×",
            onSpeedClick = {
                val nextSpeedIndex = (speedIndex + 1) % speeds.size
                speedIndex = nextSpeedIndex
                controller.setSpeed(speeds[nextSpeedIndex])
            },
            activity = activity,
            seekSeconds = doubleTapSeekSeconds,
            gestureFeedback = gestureFeedback,
            onBrightnessPercentChange = onBrightnessPercentChange,
            onVolumePercentChange = onVolumePercentChange,
            onGestureFeedback = onGestureFeedback,
            onSeekGesture = onSeekGesture
        )
    } else {
        var portraitControlsVisible by remember { mutableStateOf(true) }
        var portraitTouchActive by remember { mutableStateOf(false) }

        LaunchedEffect(portraitTouchActive) {
            if (!portraitTouchActive) {
                delay(5_000)
                portraitControlsVisible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerBackground)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        portraitTouchActive = true
                        portraitControlsVisible = true
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                        portraitTouchActive = false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (portraitControlsVisible) {
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

                }

                // Video and right-side overlay buttons stay in PlayerVideoArea.kt
                PlayerVideoArea(
            player = controller.player,
            activity = activity,
            seekSeconds = doubleTapSeekSeconds,
            gestureFeedback = gestureFeedback,
            onBrightnessPercentChange = onBrightnessPercentChange,
            onVolumePercentChange = onVolumePercentChange,
            onGestureFeedback = onGestureFeedback,
            onSeekGesture = onSeekGesture,
            isLandscape = false,
            headphoneModeEnabled = headphoneModeEnabled,
            onToggleHeadphoneMode = toggleHeadphoneMode,
            orientationLocked = orientationLocked,
            onToggleOrientationLock = toggleOrientationLock,
            showOverlayControls = portraitControlsVisible,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

                if (portraitControlsVisible) {
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

                // Playback-speed control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            val nextSpeedIndex = (speedIndex + 1) % speeds.size
                            speedIndex = nextSpeedIndex
                            controller.setSpeed(speeds[nextSpeedIndex])
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
        }
    }
}

