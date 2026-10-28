package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    onPlayVideo: () -> Unit = {},
    isFavourite: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var showRename by remember(item.uri) { mutableStateOf(false) }
    var showDelete by remember(item.uri) { mutableStateOf(false) }
    var showDetails by remember(item.uri) { mutableStateOf(false) }

    val displayedFolderName = folderPath
        .substringAfterLast('/')
        .ifBlank { item.folderName }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isVideo) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavBackground)
                }
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isVideo) {
            VideoThumbnail(
                item = item,
                modifier = Modifier
                    // VIDEO THUMBNAIL WIDTH: change 160.dp below to make it wider or narrower.
                    .width(160.dp)
                    // VIDEO THUMBNAIL HEIGHT: change 100.dp below to make it taller or shorter.
                    .height(100.dp)
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
                ),
            verticalArrangement = Arrangement.spacedBy(if (isVideo) 3.dp else 0.dp)
        ) {
            if (isVideo) {
                Text(
                    text = item.name,
                    color = PrimaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 3.dp)
                )

                val qualityHeight = when {
                    item.width > 0 && item.height > 0 -> minOf(item.width, item.height)
                    item.width > 0 -> item.width
                    item.height > 0 -> item.height
                    else -> 0
                }
                val quality = if (qualityHeight > 0) "${qualityHeight}p" else ""
                Text(
                    text = listOf(quality, formatMediaSize(item.size))
                        .filter { it.isNotBlank() }
                        .joinToString("  •  "),
                    color = SecondaryText,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (displayedFolderName.isNotBlank()) {
                        Text(
                            text = displayedFolderName,
                            color = SecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }

                    MediaItemMenu(
                        onRename = { showRename = true },
                        onDelete = { showDelete = true },
                        onFavourite = onFavourite,
                        onAddToPlaylist = onAddToPlaylist,
                        onShowDetails = { showDetails = true },
                        isFavourite = isFavourite,
                        onRemoveFromPlaylist = onRemoveFromPlaylist
                    )
                }
            } else {
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
                        onShowDetails = { showDetails = true },
                        isFavourite = isFavourite,
                        onRemoveFromPlaylist = onRemoveFromPlaylist
                    )
                }

                Text(
                    text = formatMediaSize(item.size),
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