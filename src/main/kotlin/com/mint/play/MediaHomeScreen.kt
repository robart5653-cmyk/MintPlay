package com.mint.play

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MediaHomeScreen(
    isVideo: Boolean,
    permissionGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onVideoPlayerVisibilityChanged: (Boolean) -> Unit = {},
    audioPlayerController: AudioPlayerController,
    showAudioPlayer: Boolean,
    onAudioPlayerVisibilityChanged: (Boolean) -> Unit,
    onAudioSelected: (List<MediaItem>, Int) -> Unit
) {
    val context = LocalContext.current
    val historyPreferences = remember(context) {
        context.getSharedPreferences(
            MediaHistoryStore.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
    }
    var showHistory by remember(context) {
        mutableStateOf(MediaHistoryStore.isHistoryEnabled(context))
    }
    var historyUris by remember(context) {
        mutableStateOf(MediaHistoryStore.getRecentVideoUris(context))
    }

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

    var mediaSearchText by remember(isVideo) {
        mutableStateOf("")
    }

    var isLibraryDropdownExpanded by remember(isVideo) {
        mutableStateOf(false)
    }

    var selectedVideo by remember(isVideo) {
        mutableStateOf<MediaItem?>(null)
    }

    // Update the app shell in the same click event to avoid a one-frame flash
    // of the home bars before the player takes over.
    val openVideo: (MediaItem) -> Unit = { item ->
        onVideoPlayerVisibilityChanged(true)
        selectedVideo = item
    }
    val closeVideo: () -> Unit = {
        onVideoPlayerVisibilityChanged(false)
        selectedVideo = null
    }

    var showFullHistory by remember(isVideo) {
        mutableStateOf(false)
    }

    DisposableEffect(historyPreferences, selectedVideo) {
        showHistory = MediaHistoryStore.isHistoryEnabled(context)
        historyUris = MediaHistoryStore.getRecentVideoUris(context)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                MediaHistoryStore.SHOW_HISTORY_KEY -> {
                    showHistory = MediaHistoryStore.isHistoryEnabled(context)
                }
                MediaHistoryStore.VIDEO_HISTORY_KEY -> {
                    // Don't recompose the player playlist while a video is playing.
                    if (selectedVideo == null) {
                        historyUris = MediaHistoryStore.getRecentVideoUris(context)
                    }
                }
            }
        }
        historyPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            historyPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(selectedVideo, isVideo) {
        onVideoPlayerVisibilityChanged(isVideo && selectedVideo != null)
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
                            val scan = MediaLibrary.getVideoScan(context)
                            if (scan.complete) {
                                ThumbnailCache.cleanupIfLibraryChanged(context, scan.items)
                            }
                            scan.items
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
            closeVideo()
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

    val filteredVisibleItems = visibleItems.filter { item ->
        item.name.contains(mediaSearchText.trim(), ignoreCase = true)
    }

    val historyVideos = if (isVideo) {
        val itemsByUri = mediaItems.associateBy { it.uri.toString() }
        historyUris.mapNotNull { uri -> itemsByUri[uri] }
    } else {
        emptyList()
    }

    if (isVideo && selectedVideo != null) {
        val video = selectedVideo!!

        val playlistVideos = filteredVisibleItems.filter { item ->
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
        closeVideo()
    }
)
    } else if (!isVideo && showAudioPlayer && audioPlayerController.currentSong != null) {
        AudioPlayerScreen(
            controller = audioPlayerController,
            onBack = { onAudioPlayerVisibilityChanged(false) }
        )
    } else if (isVideo && showFullHistory) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "‹",
                    color = Accent,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        showFullHistory = false
                    }
                )
                Text(
                    text = "History",
                    color = PrimaryText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        showFullHistory = false
                    }
                )
            }

            if (historyVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No history yet.", color = SecondaryText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = historyVideos,
                        key = { it.uri.toString() }
                    ) { item ->
                        MediaItemRow(
                            item = item,
                            isVideo = true,
                            onPlayVideo = { openVideo(item) },
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
                            },
                            isFavourite = item.uri.toString() in favouriteUris
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (isVideo && showHistory && historyVideos.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "History  ›",
                        color = PrimaryText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            showFullHistory = true
                        }
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(
                            items = historyVideos,
                            key = { it.uri.toString() }
                        ) { item ->
                            VideoThumbnail(
                                item = item,
                                modifier = Modifier
                                    .width(128.dp)
                                    .aspectRatio(16f / 9f)
                                    .clickable { openVideo(item) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            MediaLibraryDropdown(
                selectedName = selectedCollectionName,
                playlists = playlists,
                selectedCollection = selectedCollection,
                onExpandedChange = { isLibraryDropdownExpanded = it },
                modifier = Modifier.width(if (isLibraryDropdownExpanded) 240.dp else 104.dp),
                onSelectAll = {
                    selectedCollection = MediaCollection.All
                    selectedCollectionName = "All"
                    mediaSearchText = ""
                },
                onSelectFavourites = {
                    selectedCollection = MediaCollection.Favourites
                    selectedCollectionName = "Favourites"
                    mediaSearchText = ""
                },
                onSelectPlaylist = { name ->
                    selectedCollection = MediaCollection.Playlist
                    selectedCollectionName = name
                    mediaSearchText = ""
                },
                onCreatePlaylist = {
                    dialogState.requestCreatePlaylist()
                },
                onRenamePlaylist = { oldName, newName ->
                    when (val result = actions.renamePlaylist(oldName, newName)) {
                        MediaActionResult.Success -> {
                            playlists = actions.getPlaylistNames()
                            if (selectedCollection == MediaCollection.Playlist &&
                                selectedCollectionName == oldName
                            ) {
                                selectedCollectionName = newName
                            }
                            actionMessage = "Playlist renamed."
                            reloadLibrary()
                        }
                        is MediaActionResult.Failure -> actionMessage = result.message
                        is MediaActionResult.NeedsUserApproval -> {
                            actionMessage = "Android approval is not needed to rename a playlist."
                        }
                    }
                },
                onDeletePlaylist = { name ->
                    when (val result = actions.deletePlaylist(name)) {
                        MediaActionResult.Success -> {
                            playlists = actions.getPlaylistNames()
                            if (selectedCollection == MediaCollection.Playlist &&
                                selectedCollectionName == name
                            ) {
                                selectedCollection = MediaCollection.All
                                selectedCollectionName = "All"
                            }
                            actionMessage = "Playlist deleted."
                            reloadLibrary()
                        }
                        is MediaActionResult.Failure -> actionMessage = result.message
                        is MediaActionResult.NeedsUserApproval -> {
                            actionMessage = "Android approval is not needed to delete a playlist."
                        }
                    }
                }
            )

            if (permissionGranted && !isLibraryDropdownExpanded) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp, bottom = 4.dp)
                        .background(NavBackground, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", color = SecondaryText, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = mediaSearchText,
                        onValueChange = { mediaSearchText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
                        cursorBrush = SolidColor(Accent),
                        decorationBox = { innerTextField ->
                            Box {
                                if (mediaSearchText.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        color = SecondaryText,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (mediaSearchText.isNotEmpty()) {
                        Text(
                            text = "×",
                            color = SecondaryText,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable { mediaSearchText = "" }
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
            }

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
                            text = "Allow MintPlay to access your files to build your library.",
                            color = SecondaryText
                        )

                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Grant storage access")
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

                filteredVisibleItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mediaSearchText.isNotBlank() && visibleItems.isNotEmpty()) {
                                "No matching media found."
                            } else {
                                when (selectedCollection) {
                                    MediaCollection.All ->
                                        "No ${if (isVideo) "videos" else "audio files"} found."
                                    MediaCollection.Favourites -> "No favourites yet."
                                    MediaCollection.Playlist -> "This playlist has no available media."
                                }
                            },
                            color = SecondaryText
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(
                            items = filteredVisibleItems,
                            key = { it.uri.toString() }
                        ) { item ->
                            MediaItemRow(
                                item = item,
                                isVideo = isVideo,
                                onPlayVideo = {
                                    if (isVideo) {
                                        openVideo(item)
                                    }
                                },
                                onPlayAudio = {
                                    if (!isVideo) {
                                        val audioQueue = filteredVisibleItems.filter {
                                            it.uri.toString().isNotBlank()
                                        }
                                        val audioIndex = audioQueue.indexOfFirst {
                                            it.uri == item.uri
                                        }
                                        if (audioIndex >= 0) {
                                            onAudioSelected(audioQueue, audioIndex)
                                        }
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
                                },
                                isFavourite = item.uri.toString() in favouriteUris,
                                onRemoveFromPlaylist = if (selectedCollection == MediaCollection.Playlist) {
                                    {
                                        actionHandler.removeFromPlaylist(selectedCollectionName, item.uri)
                                    }
                                } else {
                                    null
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