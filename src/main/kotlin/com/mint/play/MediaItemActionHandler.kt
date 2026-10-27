package com.mint.play

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class MediaItemActionHandler(context: Context) {

    private val actions = MediaItemActions(context.applicationContext)

    private var launchApproval: ((IntentSenderRequest) -> Unit)? = null

    private var pendingAction:
        ((Boolean) -> MediaActionResult)? = null

    private var pendingApprovalCompletion: (() -> Unit)? = null

    var onLibraryChanged: () -> Unit = {}
    var onMessage: (String) -> Unit = {}

    fun rename(uri: Uri, newName: String) {
        runAction { approved ->
            actions.renameMedia(uri, newName, approved)
        }
    }

    fun delete(uri: Uri) {
        runAction { approved ->
            actions.deleteMedia(uri, approved)
        }
    }

    fun toggleFavourite(uri: Uri): Boolean {
        val favourite = actions.toggleFavourite(uri)

        onMessage(
            if (favourite) {
                "Added to favourites."
            } else {
                "Removed from favourites."
            }
        )

        onLibraryChanged()
        return favourite
    }

    fun addToPlaylist(playlistName: String, uri: Uri) {
        handleResult(actions.addToPlaylist(playlistName, uri))
    }

    fun removeFromPlaylist(playlistName: String, uri: Uri) {
        actions.removeFromPlaylist(playlistName, uri)
        onMessage("Removed from playlist.")
        onLibraryChanged()
    }

    fun createPlaylist(name: String) {
        handleResult(actions.createPlaylist(name))
    }

    fun renamePlaylist(oldName: String, newName: String) {
        handleResult(actions.renamePlaylist(oldName, newName))
    }

    fun deletePlaylist(name: String) {
        handleResult(actions.deletePlaylist(name))
    }

    fun getPlaylistNames(): List<String> =
        actions.getPlaylistNames()

    fun getFavourites(): Set<String> =
        actions.getFavourites()

    fun getPlaylistMedia(name: String): Set<String> =
        actions.getPlaylistMedia(name)

    fun connectApprovalLauncher(
        launch: (IntentSenderRequest) -> Unit
    ) {
        launchApproval = launch
    }

    fun onApprovalResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            pendingAction = null
            pendingApprovalCompletion = null
            onMessage("The media change was cancelled.")
            return
        }

        val completion = pendingApprovalCompletion
        pendingApprovalCompletion = null

        if (completion != null) {
            pendingAction = null
            completion()
            onMessage("File deleted.")
            onLibraryChanged()
            return
        }

        val action = pendingAction
        pendingAction = null

        if (action != null) {
            handleResult(action(true), action)
        } else {
            onLibraryChanged()
        }
    }

    private fun runAction(
        action: (Boolean) -> MediaActionResult
    ) {
        handleResult(action(false), action)
    }

    private fun handleResult(
        result: MediaActionResult,
        retryAction: ((Boolean) -> MediaActionResult)? = null
    ) {
        when (result) {
            is MediaActionResult.Success -> {
                onMessage("Done.")
                onLibraryChanged()
            }

            is MediaActionResult.Failure -> {
                onMessage(result.message)
            }

            is MediaActionResult.NeedsUserApproval -> {
                val launch = launchApproval

                if (launch == null) {
                    pendingAction = null
                    pendingApprovalCompletion = null
                    onMessage("Unable to open the Android approval screen.")
                    return
                }

                if (result.operationCompletedAfterApproval) {
                    pendingAction = null
                    pendingApprovalCompletion = result.onApproved ?: {}
                } else {
                    pendingAction = retryAction
                    pendingApprovalCompletion = null
                }

                val request = IntentSenderRequest.Builder(
                    result.intentSender
                ).build()

                launch(request)
            }
        }
    }
}

@Composable
fun rememberMediaItemActionHandler(
    onLibraryChanged: () -> Unit,
    onMessage: (String) -> Unit
): MediaItemActionHandler {
    val context = LocalContext.current

    val handler = remember(context) {
        MediaItemActionHandler(context)
    }

    val approvalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        handler.onApprovalResult(result.resultCode)
    }

    SideEffect {
        handler.onLibraryChanged = onLibraryChanged
        handler.onMessage = onMessage

        handler.connectApprovalLauncher { request ->
            approvalLauncher.launch(request)
        }
    }

    return handler
}