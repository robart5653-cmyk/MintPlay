package com.mint.play

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val folderName: String = "",
    val dateModified: Long = 0L
)

object MediaLibrary {

    fun hasVideoPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return context.checkSelfPermission(permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return context.checkSelfPermission(permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun getVideos(context: Context): List<MediaItem> = getVideoScan(context).items

    fun getVideoScan(context: Context): MediaLibraryScan {
        if (!hasVideoPermission(context)) {
            return MediaLibraryScan(emptyList(), complete = false)
        }
        return queryAllVolumes(context, isVideo = true)
    }

    fun getAudio(context: Context): List<MediaItem> {
        if (!hasAudioPermission(context)) return emptyList()

        return queryAllVolumes(context, isVideo = false).items
    }

    private fun queryAllVolumes(
        context: Context,
        isVideo: Boolean
    ): MediaLibraryScan {
        val collections = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.getExternalVolumeNames(context).map { volume ->
                if (isVideo) {
                    MediaStore.Video.Media.getContentUri(volume)
                } else {
                    MediaStore.Audio.Media.getContentUri(volume)
                }
            }
        } else {
            listOf(
                if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            )
        }

        val results = linkedMapOf<String, MediaItem>()
        var complete = collections.isNotEmpty()

        for (collection in collections) {
            val scan = queryCollection(
                context = context,
                collection = collection,
                isVideo = isVideo
            )
            if (!scan.complete) complete = false
            scan.items.forEach { item ->
                results[item.uri.toString()] = item
            }
        }

        return MediaLibraryScan(
            items = results.values.sortedByDescending { it.id },
            complete = complete
        )
    }

    private data class CollectionScan(
        val items: List<MediaItem>,
        val complete: Boolean
    )

    private fun queryCollection(
        context: Context,
        collection: Uri,
        isVideo: Boolean
    ): CollectionScan {
        val results = mutableListOf<MediaItem>()
        var complete = false

        val durationColumn = if (isVideo) {
            MediaStore.Video.VideoColumns.DURATION
        } else {
            MediaStore.Audio.AudioColumns.DURATION
        }

        val folderColumn = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            durationColumn,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (isVideo) {
            projection += MediaStore.Video.VideoColumns.WIDTH
            projection += MediaStore.Video.VideoColumns.HEIGHT
        }
        projection += folderColumn

        try {
            val cursor = context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )
            if (cursor != null) {
                complete = true
                cursor.use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val durationIndex =
                        cursor.getColumnIndexOrThrow(durationColumn)
                    val dateModifiedIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val widthIndex = if (isVideo) {
                        cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.WIDTH)
                    } else {
                        -1
                    }
                    val heightIndex = if (isVideo) {
                        cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.HEIGHT)
                    } else {
                        -1
                    }
                    val folderIndex = cursor.getColumnIndexOrThrow(folderColumn)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val name = cursor.getString(nameIndex) ?: "Unknown"
                        val size = cursor.getLong(sizeIndex)
                        val duration = cursor.getLong(durationIndex)
                        val dateModified = cursor.getLong(dateModifiedIndex)
                        val width = if (isVideo) cursor.getInt(widthIndex) else 0
                        val height = if (isVideo) cursor.getInt(heightIndex) else 0
                        val storedPath = cursor.getString(folderIndex).orEmpty().trimEnd('/')
                        val folderName = if (Build.VERSION.SDK_INT >= 29) {
                            storedPath.substringAfterLast('/')
                        } else {
                            storedPath.substringBeforeLast('/').substringAfterLast('/')
                        }

                        val uri = ContentUris.withAppendedId(collection, id)

                        results.add(
                            MediaItem(
                                id = id,
                                name = name,
                                uri = uri,
                                duration = duration,
                                size = size,
                                width = width,
                                height = height,
                                folderName = folderName,
                                dateModified = dateModified
                            )
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            complete = false
            // A volume may not be accessible with the current permissions.
        } catch (_: IllegalArgumentException) {
            // Skip a volume whose provider doesn't support this query.
            complete = false
        }

        return CollectionScan(results, complete)
    }
}

data class MediaLibraryScan(
    val items: List<MediaItem>,
    val complete: Boolean
)