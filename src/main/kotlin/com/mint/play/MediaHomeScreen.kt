package com.mint.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MediaHomeScreen(
    isVideo: Boolean,
    permissionGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = remember(context) { MediaItemActions(context) }
    val dialogState = remember { MediaActionDialogState() }

    var mediaItems by remember(isVideo) {
        mutableStateOf<List<MediaItem>>(emptyList())
    }

    var loading by remember(isVideo) {
        mutableStateOf(false)
    }

    var selectedCollection by remember(isVideo) {
        mutableStateOf(MediaCollection.All)
    }

    var selectedCollectionName by remember(isVideo) {
        mutableStateOf("All")
    }

    var selectedVideo by remember(isVideo) {
        mutableStateOf<MediaItem?>(null)
    }

    var favouriteUris by remember {
        mutableStateOf(actions.getFavourites())
    }

    var playlists by remember {
        mutableStateOf(actions.getPlaylistNames())
    }

    var actionMessage by remember {
        mutableStateOf<String?>(null)
    }

    val reloadLibrary: () -> Unit = {
        if (permissionGranted) {
            scope.launch {
                loading = true

                try {
                    mediaItems = withContext(Dispatchers.IO) {
                        if (isVideo) {
                            MediaLibrary.getVideos(context)
                        } else {
                            MediaLibrary.getAudio(context)
                        }
                    }

                    favouriteUris = actions.getFavourites()
                    playlists = actions.getPlaylistNames()
                } finally {
                    loading = false
                }
            }
        }
    }

    val actionHandler = rememberMediaItemActionHandler(
        onLibraryChanged = reloadLibrary,
        onMessage = { actionMessage = it }
    )

    LaunchedEffect(permissionGranted, isVideo) {
        if (permissionGranted) {
            reloadLibrary()
        } else {
            mediaItems = emptyList()
            selectedVideo = null
            loading = false
        }
    }

    val visibleItems = when (selectedCollection) {
        MediaCollection.All -> mediaItems

        MediaCollection.Favourites -> mediaItems.filter {
            it.uri.toString() in favouriteUris
        }

        MediaCollection.Playlist -> {
            val playlistUris =
                actions.getPlaylistMedia(selectedCollectionName)

            mediaItems.filter {
                it.uri.toString() in playlistUris
            }
        }
    }

    if (isVideo && selectedVideo != null) {
        val video = selectedVideo!!

        val playlistVideos = visibleItems.filter { item ->
            item.uri.toString().isNotBlank()
        }

        val playerVideos = if (
            playlistVideos.any { it.uri == video.uri }
        ) {
            playlistVideos
        } else {
            listOf(video) + playlistVideos
        }

        val playerIndex = playerVideos.indexOfFirst {
            it.uri == video.uri
        }.coerceAtLeast(0)

        VideoPlayerScreen(
    videoUris = playerVideos.map { it.uri },
    videoTitles = playerVideos.map { it.name },
    initialIndex = playerIndex,
    onBack = {
        // Keep the same back-navigation action
        // that your existing call was using.
    }
)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            MediaLibraryDropdown(
                selectedName = selectedCollectionName,
                playlists = playlists,
                selectedCollection = selectedCollection,
                modifier = Modifier.padding(top = 8.dp),
                onSelectAll = {
                    selectedCollection = MediaCollection.All
                    selectedCollectionName = "All"
                },
                onSelectFavourites = {
                    selectedCollection = MediaCollection.Favourites
                    selectedCollectionName = "Favourites"
                },
                onSelectPlaylist = { name ->
                    selectedCollection = MediaCollection.Playlist
                    selectedCollectionName = name
                },
                onCreatePlaylist = {
                    dialogState.requestCreatePlaylist()
                }
            )

            actionMessage?.let { message ->
                Text(
                    text = message,
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            when {
                !permissionGranted -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Allow access to your ${
                                if (isVideo) "videos" else "audio"
                            } to build your library.",
                            color = SecondaryText
                        )

                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Grant access")
                        }
                    }
                }

                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                }

                visibleItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (selectedCollection) {
                                MediaCollection.All ->
                                    "No ${
                                        if (isVideo) "videos" else "audio files"
                                    } found."

                                MediaCollection.Favourites ->
                                    "No favourites yet."

                                MediaCollection.Playlist ->
                                    "This playlist has no available media."
                            },
                            color = SecondaryText
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = visibleItems,
                            key = { it.uri.toString() }
                        ) { item ->
                            MediaItemRow(
                                item = item,
                                isVideo = isVideo,
                                onPlayVideo = {
                                    if (isVideo) {
                                        selectedVideo = item
                                    }
                                },
                                onFavourite = {
                                    actionHandler.toggleFavourite(item.uri)
                                    favouriteUris = actions.getFavourites()
                                },
                                onAddToPlaylist = {
                                    dialogState.requestAddToPlaylist(item.uri)
                                },
                                onRename = { newName ->
                                    actionHandler.rename(item.uri, newName)
                                },
                                onDelete = {
                                    actionHandler.delete(item.uri)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    MediaItemActionDialogs(
        state = dialogState,
        playlists = playlists,
        onCreatePlaylist = { name ->
            actionHandler.createPlaylist(name)
            playlists = actions.getPlaylistNames()
        },
        onAddToPlaylist = { name, uri ->
            actionHandler.addToPlaylist(name, uri)
        }
    )
}