package com.mint.play

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem as ExoMediaItem

class AudioPlayerController(context: Context) {
    private val appContext = context.applicationContext
    private var playerInstance: ExoPlayer? = null

    val player: ExoPlayer
        get() = playerInstance ?: ExoPlayer.Builder(appContext).build().also { created ->
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            created.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                    syncCurrentTrack()
                    refreshProgress()
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    refreshProgress()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    shuffleEnabled = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatModeValue: Int) {
                    repeatMode = repeatModeValue
                }
            })
            playerInstance = created
        }

    val hasPlayer: Boolean get() = playerInstance != null

    var queue by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var currentIndex by mutableIntStateOf(-1)
        private set
    val currentSong: MediaItem? get() = queue.getOrNull(currentIndex)

    var isPlaying by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var shuffleEnabled by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    fun playQueue(items: List<MediaItem>, startIndex: Int) {
        if (items.isEmpty()) return

        queue = items
        currentIndex = startIndex.coerceIn(0, items.lastIndex)
        val activePlayer = player
        activePlayer.shuffleModeEnabled = shuffleEnabled
        activePlayer.repeatMode = repeatMode
        activePlayer.setMediaItems(
            items.map { ExoMediaItem.fromUri(it.uri) },
            currentIndex,
            0L
        )
        activePlayer.prepare()
        activePlayer.play()
        refreshProgress()
    }

    fun playAt(index: Int) {
        if (index !in queue.indices) return
        player.seekTo(index, 0L)
        player.play()
        syncCurrentTrack()
        refreshProgress()
    }

    fun togglePlayPause() {
        val activePlayer = playerInstance ?: return
        if (activePlayer.mediaItemCount == 0) return
        if (activePlayer.isPlaying || activePlayer.playWhenReady) {
            activePlayer.pause()
        } else {
            if (activePlayer.playbackState == Player.STATE_ENDED) activePlayer.seekTo(0L)
            activePlayer.play()
        }
        refreshProgress()
    }

    fun previous() {
        val activePlayer = playerInstance ?: return
        if (activePlayer.mediaItemCount == 0) return
        if (activePlayer.currentPosition > 3_000L) {
            activePlayer.seekTo(0L)
        } else if (activePlayer.hasPreviousMediaItem()) {
            activePlayer.seekToPreviousMediaItem()
        } else {
            activePlayer.seekTo(0L)
        }
        refreshProgress()
    }

    fun next() {
        val activePlayer = playerInstance ?: return
        if (activePlayer.hasNextMediaItem()) {
            activePlayer.seekToNextMediaItem()
        } else if (activePlayer.mediaItemCount > 0 && activePlayer.repeatMode != Player.REPEAT_MODE_OFF) {
            activePlayer.seekTo(0L)
        }
        refreshProgress()
    }

    fun toggleShuffle() {
        val activePlayer = playerInstance ?: return
        activePlayer.shuffleModeEnabled = !activePlayer.shuffleModeEnabled
        shuffleEnabled = activePlayer.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val activePlayer = playerInstance ?: return
        activePlayer.repeatMode = when (activePlayer.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        repeatMode = activePlayer.repeatMode
    }

    fun seekTo(milliseconds: Long) {
        val activePlayer = playerInstance ?: return
        activePlayer.seekTo(milliseconds.coerceIn(0L, activePlayer.duration.coerceAtLeast(0L)))
        refreshProgress()
    }

    fun refreshProgress() {
        val activePlayer = playerInstance ?: return
        syncCurrentTrack()
        position = activePlayer.currentPosition.coerceAtLeast(0L)
        duration = activePlayer.duration.takeIf { it > 0L } ?: 0L
        isPlaying = activePlayer.isPlaying
        shuffleEnabled = activePlayer.shuffleModeEnabled
        repeatMode = activePlayer.repeatMode
    }

    private fun syncCurrentTrack() {
        val activePlayer = playerInstance ?: return
        val index = activePlayer.currentMediaItemIndex
        currentIndex = if (queue.isEmpty() || index !in queue.indices) -1 else index
    }

    fun pauseForVideo(): Boolean {
        val activePlayer = playerInstance ?: return false
        val wasPlaying = activePlayer.isPlaying || activePlayer.playWhenReady
        if (wasPlaying) activePlayer.pause()
        refreshProgress()
        return wasPlaying
    }

    fun resumeAfterVideo() {
        val activePlayer = playerInstance ?: return
        if (activePlayer.mediaItemCount == 0) return
        activePlayer.play()
        refreshProgress()
    }

    fun release() {
        playerInstance?.release()
        playerInstance = null
        queue = emptyList()
        currentIndex = -1
        isPlaying = false
        position = 0L
        duration = 0L
    }
}
