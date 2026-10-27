package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DialogSurface = Color(0xFF1A1E25)
private val DeleteColor = Color(0xFFFF7777)
private val MintPlayGreen = Color(0xFF4DD4AC)

@Composable
fun MediaItemDialogs(
    item: MediaItem,
    isVideo: Boolean,
    folderPath: String,
    showRename: Boolean,
    showDelete: Boolean,
    showDetails: Boolean,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissDetails: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var newName by remember(item.uri, item.name) {
        mutableStateOf(item.name)
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = onDismissRename,
            containerColor = DialogSurface,
            title = {
                Text("Rename file", color = PrimaryText)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter a new name.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    CompositionLocalProvider(
                        LocalTextSelectionColors provides TextSelectionColors(
                            handleColor = Color.Transparent,
                            backgroundColor = Color(0x664DD4AC)
                        )
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = PrimaryText,
                                fontSize = 15.sp
                            ),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintPlayGreen,
                                unfocusedBorderColor = MintPlayGreen,
                                cursorColor = MintPlayGreen,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanedName = newName.trim()
                        if (
                            cleanedName.isNotBlank() &&
                            cleanedName != item.name
                        ) {
                            onRename(cleanedName)
                        }
                        onDismissRename()
                    }
                ) {
                    Text("Save", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRename) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            containerColor = DialogSurface,
            title = {
                Text("Delete file?", color = PrimaryText)
            },
            text = {
                Text(
                    text = "Delete \"${item.name}\"? This action may not be reversible.",
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissDelete()
                        onDelete()
                    }
                ) {
                    Text("Delete", color = DeleteColor)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = onDismissDetails,
            containerColor = DialogSurface,
            title = {
                Text("File details", color = PrimaryText)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailLine("Name", item.name)

                    if (isVideo) {
                        DetailLine(
                            "Duration",
                            formatMediaDuration(item.duration)
                        )
                    }

                    DetailLine("Size", formatMediaSize(item.size))

                    if (folderPath.isNotBlank()) {
                        DetailLine("Folder", folderPath)
                    }

                    DetailLine("URI", item.uri.toString())
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissDetails) {
                    Text("Close", color = Accent)
                }
            }
        )
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = Accent,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 13.sp
        )
    }
}