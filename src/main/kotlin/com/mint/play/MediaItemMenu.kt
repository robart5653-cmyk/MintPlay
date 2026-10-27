package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val MenuSurface = Color(0xFF1A1E25)
private val MenuDivider = Color(0xFF303640)
private val DeleteColor = Color(0xFFFF7777)

@Composable
fun MediaItemMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onFavourite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit,
    isFavourite: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SecondaryText)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(228.dp)
                .background(MenuSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            MenuAction(
                icon = "✎",
                title = "Rename",
                subtitle = "Change file name",
                onClick = {
                    expanded = false
                    onRename()
                }
            )

            MenuAction(
                icon = "×",
                title = "Delete",
                subtitle = "Remove this file",
                iconColor = DeleteColor,
                titleColor = DeleteColor,
                onClick = {
                    expanded = false
                    onDelete()
                }
            )

            HorizontalDivider(
                color = MenuDivider,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 5.dp
                )
            )

            MenuAction(
                icon = if (isFavourite) "★" else "♡",
                title = if (isFavourite) "Remove from favourites" else "Add to favourites",
                subtitle = if (isFavourite) "Take it out of favourites" else "Keep it close",
                onClick = {
                    expanded = false
                    onFavourite()
                }
            )

            MenuAction(
                icon = "♫",
                title = "Add to playlist",
                subtitle = "Organize your library",
                onClick = {
                    expanded = false
                    onAddToPlaylist()
                }
            )

            if (onRemoveFromPlaylist != null) {
                MenuAction(
                    icon = "−",
                    title = "Remove from playlist",
                    subtitle = "Keep the file in your library",
                    onClick = {
                        expanded = false
                        onRemoveFromPlaylist()
                    }
                )
            }

            MenuAction(
                icon = "ⓘ",
                title = "File details",
                subtitle = "View media information",
                onClick = {
                    expanded = false
                    onShowDetails()
                }
            )
        }
    }
}

@Composable
private fun MenuAction(
    icon: String,
    title: String,
    subtitle: String,
    iconColor: Color = Accent,
    titleColor: Color = PrimaryText,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (titleColor == DeleteColor) {
                            Color(0xFF302021)
                        } else {
                            Color(0xFF1D2928)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    color = iconColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 5.dp)
    )
}