package com.mint.play

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.yausername.youtubedl_android.YoutubeDL
import java.util.Locale

internal data class OnlineVideoMetadata(
    val title: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val maximumQuality: String?,
    val firstFrame: Bitmap?,
    val bestStreamUrl: String?,
    val streamHeaders: Map<String, String>
)

internal fun extractOnlineVideoMetadata(context: Context, url: String): OnlineVideoMetadata {
    YoutubeDL.getInstance().init(context.applicationContext)
    val info = YoutubeDL.getInstance().getInfo(url)
    val videoFormats = info.formats.orEmpty().filter { it.height > 0 && it.vcodec != "none" }
    val bestFormat = videoFormats.maxByOrNull { it.height }
    val thumbnailUrl = info.thumbnail?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    val streamUrl = bestFormat?.url
    val streamHeaders = bestFormat?.httpHeaders ?: emptyMap()
    val firstFrame = if (thumbnailUrl == null && streamUrl != null) {
        extractVideoFrame(streamUrl, streamHeaders)
    } else null
    val duration = info.duration.toLong().takeIf { it > 0L }?.let { seconds ->
        val hours = seconds / 3600L
        val minutes = (seconds % 3600L) / 60L
        val remaining = seconds % 60L
        if (hours > 0L) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remaining)
        else String.format(Locale.ROOT, "%d:%02d", minutes, remaining)
    }
    return OnlineVideoMetadata(
        title = info.title ?: info.fulltitle,
        durationText = duration,
        thumbnailUrl = thumbnailUrl,
        maximumQuality = bestFormat?.height?.let { "${it}p" },
        firstFrame = firstFrame,
        bestStreamUrl = streamUrl,
        streamHeaders = streamHeaders
    )
}

internal fun extractVideoFrame(url: String, headers: Map<String, String>): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(url, headers)
        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}
