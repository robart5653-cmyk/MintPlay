package com.mint.play

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class VideoPlayerController(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(
        context.applicationContext
    ).build()

    fun play(uri: Uri) {
        val currentUri = player.currentMediaItem
            ?.localConfiguration
            ?.uri

        if (currentUri == uri) {
            if (!player.isPlaying) {
                player.play()
            }
            return
        }

        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    fun setAudioOnlyMode(enabled: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, enabled)
            .build()
    }

    fun togglePlayPause() {
        if (player.isPlaying || player.playWhenReady) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
        }
    }

    fun seekBy(milliseconds: Long) {
        val duration = player.duration
        val target = (player.currentPosition + milliseconds)
            .coerceAtLeast(0L)

        player.seekTo(
            if (duration > 0L) target.coerceAtMost(duration)
            else target
        )
    }

    fun seekTo(milliseconds: Long) {
        player.seekTo(milliseconds.coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun release() {
        player.release()
    }
}