package com.mint.play

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

sealed class MediaActionResult {
    object Success : MediaActionResult()

    data class Failure(
        val message: String
    ) : MediaActionResult()

    data class NeedsUserApproval(
        val intentSender: IntentSender,
        val operationCompletedAfterApproval: Boolean = false,
        val onApproved: (() -> Unit)? = null
    ) : MediaActionResult()
}

class MediaItemActions(context: Context) {

    private val resolver = context.contentResolver

    private val preferences = context.getSharedPreferences(
        "mintplay_media_actions",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val FAVOURITES_KEY = "media_favourites"
        private const val PLAYLISTS_KEY = "media_playlists"
        private const val PLAYLIST_PREFIX = "playlist_"
    }

    private fun readStringSet(key: String): MutableSet<String> {
        return preferences.getStringSet(key, emptySet<String>())
            ?.toMutableSet()
            ?: mutableSetOf()
    }

    private fun writeStringSet(key: String, values: Set<String>) {
        preferences.edit()
            .putStringSet(key, values.toSet())
            .apply()
    }

    fun getFavourites(): Set<String> =
        readStringSet(FAVOURITES_KEY).toSet()

    fun isFavourite(uri: Uri): Boolean =
        uri.toString() in readStringSet(FAVOURITES_KEY)

    fun toggleFavourite(uri: Uri): Boolean {
        val favourites = readStringSet(FAVOURITES_KEY)
        val key = uri.toString()

        val added = if (favourites.contains(key)) {
            favourites.remove(key)
            false
        } else {
            favourites.add(key)
            true
        }

        writeStringSet(FAVOURITES_KEY, favourites)
        return added
    }

    fun removeFavourite(uri: Uri) {
        val favourites = readStringSet(FAVOURITES_KEY)
        favourites.remove(uri.toString())
        writeStringSet(FAVOURITES_KEY, favourites)
    }

    fun getPlaylistNames(): List<String> =
        readStringSet(PLAYLISTS_KEY).toList().sorted()

    fun createPlaylist(name: String): MediaActionResult {
        val cleaned = name.trim()

        if (cleaned.isEmpty()) {
            return MediaActionResult.Failure("Enter a playlist name.")
        }

        val playlists = readStringSet(PLAYLISTS_KEY)

        if (playlists.any { it.equals(cleaned, ignoreCase = true) }) {
            return MediaActionResult.Failure("That playlist already exists.")
        }

        playlists.add(cleaned)
        writeStringSet(PLAYLISTS_KEY, playlists)
        writeStringSet(playlistKey(cleaned), emptySet())

        return MediaActionResult.Success
    }

    fun renamePlaylist(
        oldName: String,
        newName: String
    ): MediaActionResult {
        val cleaned = newName.trim()

        if (cleaned.isEmpty()) {
            return MediaActionResult.Failure("Enter a playlist name.")
        }

        val playlists = readStringSet(PLAYLISTS_KEY)

        if (playlists.any {
                it.equals(cleaned, ignoreCase = true) &&
                    !it.equals(oldName, ignoreCase = true)
            }
        ) {
            return MediaActionResult.Failure("That playlist already exists.")
        }

        val items = readStringSet(playlistKey(oldName))
        playlists.remove(oldName)
        playlists.add(cleaned)

        preferences.edit()
            .putStringSet(PLAYLISTS_KEY, playlists.toSet())
            .putStringSet(playlistKey(cleaned), items.toSet())
            .remove(playlistKey(oldName))
            .apply()

        return MediaActionResult.Success
    }

    fun deletePlaylist(name: String): MediaActionResult {
        val playlists = readStringSet(PLAYLISTS_KEY)
        playlists.remove(name)

        preferences.edit()
            .putStringSet(PLAYLISTS_KEY, playlists.toSet())
            .remove(playlistKey(name))
            .apply()

        return MediaActionResult.Success
    }

    fun addToPlaylist(
        playlistName: String,
        uri: Uri
    ): MediaActionResult {
        val playlists = readStringSet(PLAYLISTS_KEY)

        if (playlistName !in playlists) {
            return MediaActionResult.Failure("Playlist not found.")
        }

        val items = readStringSet(playlistKey(playlistName))
        items.add(uri.toString())
        writeStringSet(playlistKey(playlistName), items)

        return MediaActionResult.Success
    }

    fun removeFromPlaylist(playlistName: String, uri: Uri) {
        val items = readStringSet(playlistKey(playlistName))
        items.remove(uri.toString())
        writeStringSet(playlistKey(playlistName), items)
    }

    fun getPlaylistMedia(playlistName: String): Set<String> =
        readStringSet(playlistKey(playlistName)).toSet()

    private fun playlistKey(name: String): String =
        PLAYLIST_PREFIX + name

    private fun removeUriFromSavedCollections(uri: Uri) {
        removeFavourite(uri)

        getPlaylistNames().forEach { name ->
            removeFromPlaylist(name, uri)
        }
    }

    fun renameMedia(
        uri: Uri,
        newName: String,
        approved: Boolean = false
    ): MediaActionResult {
        val cleaned = newName.trim()

        if (cleaned.isEmpty()) {
            return MediaActionResult.Failure("Enter a file name.")
        }

        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleaned)
            }

            val updated = resolver.update(uri, values, null, null)

            if (updated > 0) {
                MediaActionResult.Success
            } else {
                MediaActionResult.Failure("The file could not be renamed.")
            }
        } catch (e: RecoverableSecurityException) {
            MediaActionResult.NeedsUserApproval(
                e.userAction.actionIntent.intentSender
            )
        } catch (e: SecurityException) {
            requestWriteApproval(uri)
        } catch (e: Exception) {
            MediaActionResult.Failure(
                e.message ?: "The file could not be renamed."
            )
        }
    }

    fun deleteMedia(
        uri: Uri,
        approved: Boolean = false
    ): MediaActionResult {
        return try {
            val deleted = resolver.delete(uri, null, null)

            if (deleted > 0) {
                removeUriFromSavedCollections(uri)
                MediaActionResult.Success
            } else {
                MediaActionResult.Failure("The file could not be deleted.")
            }
        } catch (e: RecoverableSecurityException) {
            MediaActionResult.NeedsUserApproval(
                e.userAction.actionIntent.intentSender
            )
        } catch (e: SecurityException) {
            requestDeleteApproval(uri)
        } catch (e: Exception) {
            MediaActionResult.Failure(
                e.message ?: "The file could not be deleted."
            )
        }
    }

    private fun requestWriteApproval(uri: Uri): MediaActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return MediaActionResult.Failure(
                "Android did not allow this file to be renamed."
            )
        }

        return try {
            val request = MediaStore.createWriteRequest(
                resolver,
                listOf(uri)
            )

            MediaActionResult.NeedsUserApproval(request.intentSender)
        } catch (e: Exception) {
            MediaActionResult.Failure(
                e.message ?: "Unable to request rename permission."
            )
        }
    }

    private fun requestDeleteApproval(uri: Uri): MediaActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return MediaActionResult.Failure(
                "Android did not allow this file to be deleted."
            )
        }

        return try {
            val request = MediaStore.createDeleteRequest(
                resolver,
                listOf(uri)
            )

            MediaActionResult.NeedsUserApproval(
                intentSender = request.intentSender,
                operationCompletedAfterApproval = true,
                onApproved = {
                    removeUriFromSavedCollections(uri)
                }
            )
        } catch (e: Exception) {
            MediaActionResult.Failure(
                e.message ?: "Unable to request delete permission."
            )
        }
    }
}