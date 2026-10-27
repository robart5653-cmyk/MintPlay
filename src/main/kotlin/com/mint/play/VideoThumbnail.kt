package com.mint.play

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = item.uri,
        key2 = item.duration
    ) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(context, item.uri)

                // Duration is in milliseconds; MediaMetadataRetriever uses microseconds.
                val frameTimeUs =
                    item.duration.coerceAtLeast(0L) * 100L

                retriever.getFrameAtTime(
                    frameTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                    // Ignore release failures.
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Background)
    ) {
        bitmap?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
    }
}