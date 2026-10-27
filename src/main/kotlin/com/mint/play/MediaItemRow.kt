package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MediaItemRow(
    item: MediaItem,
    isVideo: Boolean,
    folderPath: String = "",
    onFavourite: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onShowDetails: () -> Unit = {},
    onPlayVideo: () -> Unit = {}
) {
    var showRename by remember(item.uri) { mutableStateOf(false) }
    var showDelete by remember(item.uri) { mutableStateOf(false) }
    var showDetails by remember(item.uri) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavBackground)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isVideo) {
            VideoThumbnail(
                item = item,
                modifier = Modifier
                    .width(132.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlayVideo() }
            )
        } else {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♫",
                    color = Accent,
                    fontSize = 28.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (isVideo) {
                        Modifier.clickable { onPlayVideo() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = item.name,
                    color = PrimaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 3.dp)
                )

                MediaItemMenu(
                    onRename = { showRename = true },
                    onDelete = { showDelete = true },
                    onFavourite = onFavourite,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowDetails = { showDetails = true }
                )
            }

            Text(
                text = buildString {
                    if (isVideo) {
                        append(formatMediaDuration(item.duration))
                        append("  •  ")
                    }
                    append(formatMediaSize(item.size))
                },
                color = SecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp)
            )

            if (folderPath.isNotBlank()) {
                Text(
                    text = folderPath,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }

    MediaItemDialogs(
        item = item,
        isVideo = isVideo,
        folderPath = folderPath,
        showRename = showRename,
        showDelete = showDelete,
        showDetails = showDetails,
        onDismissRename = { showRename = false },
        onDismissDelete = { showDelete = false },
        onDismissDetails = { showDetails = false },
        onRename = onRename,
        onDelete = onDelete
    )
}