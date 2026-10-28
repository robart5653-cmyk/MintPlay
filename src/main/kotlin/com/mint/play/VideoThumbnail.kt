
package com.mint.play

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        key1 = "${item.uri}|${item.duration}|${item.size}|${item.dateModified}"
    ) {
        value = withContext(Dispatchers.IO) {
            ThumbnailCache.loadOrCreate(context, item)
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
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            
Text(
    text = formatMediaDuration(item.duration),
    color = Color.White,
    fontSize = 12.sp,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Medium,
    maxLines = 1,
    modifier = Modifier.padding(0.dp)
)
        }
    }
}