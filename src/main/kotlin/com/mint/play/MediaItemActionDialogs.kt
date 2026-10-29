package com.mint.play

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MediaActionDialogState {
    var showCreatePlaylist by mutableStateOf(false)
    var showAddToPlaylist by mutableStateOf(false)
    var selectedUri by mutableStateOf<Uri?>(null)

    fun requestCreatePlaylist() {
        showCreatePlaylist = true
    }

    fun requestAddToPlaylist(uri: Uri) {
        selectedUri = uri
        showAddToPlaylist = true
    }

    fun closeCreatePlaylist() {
        showCreatePlaylist = false
    }

    fun closeAddToPlaylist() {
        showAddToPlaylist = false
        selectedUri = null
    }
}

@Composable
fun MediaItemActionDialogs(
    state: MediaActionDialogState,
    playlists: List<String>,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (String, Uri) -> Unit
) {
    var playlistName by rememberPlaylistName(state.showCreatePlaylist) 

    if (state.showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = {
                state.closeCreatePlaylist()
                playlistName = ""
            },
            containerColor = NavBackground,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = { Text("Create playlist", color = PrimaryText) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose a name for your playlist.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist name", color = SecondaryText) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Accent,
                            cursorColor = Accent,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedLabelColor = Accent,
                            unfocusedLabelColor = SecondaryText
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = playlistName.trim()
                        if (name.isNotBlank()) {
                            onCreatePlaylist(name)
                            playlistName = ""
                            state.closeCreatePlaylist()
                        }
                    }
                ) {
                    Text("Create", color = Accent)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.closeCreatePlaylist()
                        playlistName = ""
                    }
                ) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    if (state.showAddToPlaylist) {
        AlertDialog(
            onDismissRequest = { state.closeAddToPlaylist() },
            containerColor = NavBackground,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = { Text("Add to playlist", color = PrimaryText) },
            text = {
                if (playlists.isEmpty()) {
                    Text(
                        "Create a playlist first.",
                        color = SecondaryText
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        playlists.forEach { name ->
                            TextButton(
                                onClick = {
                                    val uri = state.selectedUri
                                    if (uri != null) {
                                        onAddToPlaylist(name, uri)
                                    }
                                    state.closeAddToPlaylist()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = name,
                                    modifier = Modifier
                                        .background(Accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Accent, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = PrimaryText,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { state.closeAddToPlaylist() }
                ) {
                    Text("Close", color = Accent)
                }
            }
        )
    }
}

@Composable
private fun rememberPlaylistName(
    visible: Boolean
): androidx.compose.runtime.MutableState<String> {
    return androidx.compose.runtime.remember(visible) {
        mutableStateOf("")
    }
}