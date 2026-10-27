package com.mint.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MediaCollection {
    All,
    Favourites,
    Playlist
}

@Composable
fun MediaLibraryDropdown(
    selectedName: String = "All",
    playlists: List<String> = emptyList(),
    selectedCollection: MediaCollection = MediaCollection.All,
    onSelectAll: () -> Unit = {},
    onSelectFavourites: () -> Unit = {},
    onSelectPlaylist: (String) -> Unit = {},
    onCreatePlaylist: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val filteredPlaylists = playlists.filter {
        it.contains(searchText.trim(), ignoreCase = true)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedName,
                color = PrimaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            ChevronIcon(
                expanded = expanded,
                color = SecondaryText
            )
        }

        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = NavBackground,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CollectionOption(
                        title = "All",
                        selected = selectedCollection == MediaCollection.All,
                        icon = "all",
                        onClick = {
                            onSelectAll()
                            searchText = ""
                            expanded = false
                        }
                    )

                    CollectionOption(
                        title = "Favourites",
                        selected = selectedCollection == MediaCollection.Favourites,
                        icon = "favourite",
                        onClick = {
                            onSelectFavourites()
                            searchText = ""
                            expanded = false
                        }
                    )

                    CollectionOption(
                        title = "Create new playlist",
                        selected = false,
                        icon = "create",
                        onClick = {
                            expanded = false
                            onCreatePlaylist()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                        color = Inactive.copy(alpha = 0.25f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .background(
                                Background,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchIcon(
                            color = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = PrimaryText,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Accent),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = "Search playlists",
                                            color = SecondaryText,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    if (filteredPlaylists.isEmpty()) {
                        Text(
                            text = if (playlists.isEmpty()) {
                                "No playlists yet"
                            } else {
                                "No playlists found"
                            },
                            color = SecondaryText,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 14.dp
                                )
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp)
                        ) {
                            items(
                                items = filteredPlaylists,
                                key = { it }
                            ) { playlist ->
                                CollectionOption(
                                    title = playlist,
                                    selected = selectedCollection ==
                                        MediaCollection.Playlist &&
                                        selectedName == playlist,
                                    icon = "playlist",
                                    onClick = {
                                        onSelectPlaylist(playlist)
                                        searchText = ""
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionOption(
    title: String,
    selected: Boolean,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) Accent.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (icon) {
            "all" -> Text(
                text = "≡",
                color = if (selected) Accent else SecondaryText,
                fontSize = 22.sp,
                modifier = Modifier.width(28.dp)
            )

            "favourite" -> Text(
                text = "★",
                color = if (selected) Accent else SecondaryText,
                fontSize = 18.sp,
                modifier = Modifier.width(28.dp)
            )

            "create" -> Text(
                text = "+",
                color = Accent,
                fontSize = 24.sp,
                modifier = Modifier.width(28.dp)
            )

            else -> Text(
                text = "♫",
                color = if (selected) Accent else SecondaryText,
                fontSize = 18.sp,
                modifier = Modifier.width(28.dp)
            )
        }

        Text(
            text = title,
            color = if (selected) Accent else PrimaryText,
            fontSize = 14.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            modifier = Modifier.weight(1f)
        )

        if (selected) {
            Text(
                text = "✓",
                color = Accent,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ChevronIcon(
    expanded: Boolean,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .size(18.dp)
            .rotate(if (expanded) 180f else 0f)
    ) {
        val strokeWidth = 1.8.dp.toPx()

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.25f,
                size.height * 0.38f
            ),
            end = Offset(
                size.width * 0.5f,
                size.height * 0.62f
            ),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.5f,
                size.height * 0.62f
            ),
            end = Offset(
                size.width * 0.75f,
                size.height * 0.38f
            ),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
private fun SearchIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val radius = size.minDimension * 0.30f
        val centerX = size.width * 0.42f
        val centerY = size.height * 0.42f

        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth)
        )

        drawLine(
            color = color,
            start = Offset(
                centerX + radius * 0.70f,
                centerY + radius * 0.70f
            ),
            end = Offset(
                size.width * 0.88f,
                size.height * 0.88f
            ),
            strokeWidth = strokeWidth
        )
    }
}