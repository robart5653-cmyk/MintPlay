package com.mint.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Persistent thumbnail cache used by video rows and the history strip. */
object ThumbnailCache {
    private const val PREFS_NAME = "thumbnail_cache_state"
    private const val INVENTORY_PREFIX = "inventory_"
    private const val MAX_WIDTH = 480
    private const val MAX_HEIGHT = 300

    private val memoryCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceAtLeast(1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val locks = ConcurrentHashMap<String, Any>()

    fun loadOrCreate(context: Context, item: MediaItem): Bitmap? {
        val key = cacheKey(item)
        memoryCache.get(key)?.let { return it }
        val lock = locks.computeIfAbsent(key) { Any() }

        return synchronized(lock) {
            try {
                memoryCache.get(key)?.let { return@synchronized it }

                val file = cacheDirectory(context)?.let { File(it, "thumb_$key.jpg") }
                if (file != null && file.isFile) {
                    val cached = BitmapFactory.decodeFile(file.absolutePath)
                    if (cached != null) {
                        memoryCache.put(key, cached)
                        return@synchronized cached
                    }
                    file.delete()
                }

                val frame = createThumbnail(context, item) ?: return@synchronized null
                if (file != null) saveThumbnail(frame, file)
                memoryCache.put(key, frame)
                frame
            } finally {
                locks.remove(key, lock)
            }
        }
    }

    /**
     * Called after a successful video-library scan. If the inventory is unchanged,
     * this returns before listing the cache folder or deleting anything.
     */
    fun cleanupIfLibraryChanged(context: Context, videos: List<MediaItem>) {
        val directory = cacheDirectory(context) ?: return
        if (!directory.isDirectory && !directory.mkdirs()) return

        val currentKeys = videos.map(::cacheKey).toSet()
        val signature = sha256(currentKeys.sorted().joinToString("\n"))
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stateKey = INVENTORY_PREFIX + sha256(directory.absolutePath)
        if (preferences.getString(stateKey, null) == signature) return

        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.name.endsWith(".tmp")) {
                file.delete()
            } else if (file.name.startsWith("thumb_") && file.name.endsWith(".jpg")) {
                val key = file.name.removePrefix("thumb_").removeSuffix(".jpg")
                if (key !in currentKeys) file.delete()
            }
        }

        memoryCache.snapshot().keys.filter { it !in currentKeys }.forEach { key -> memoryCache.remove(key) }
        preferences.edit().putString(stateKey, signature).apply()
    }

    private fun cacheKey(item: MediaItem): String = sha256(
        "${item.uri}\n${item.size}\n${item.duration}\n${item.dateModified}"
    )

    private fun cacheDirectory(context: Context): File? {
        // App-specific external storage: /Android/data/com.mint.play/files/thumbnail_cache
        val externalDirectory = context.getExternalFilesDir(null)
        if (externalDirectory != null) {
            val directory = File(externalDirectory, "thumbnail_cache")
            if (directory.isDirectory || directory.mkdirs()) {
                ensureNoMedia(directory)
                return directory
            }
        }

        // Keep thumbnail generation working if external storage is unavailable.
        return privateCacheDirectory(context)
    }

    private fun privateCacheDirectory(context: Context): File? {
        val directory = File(context.cacheDir, "thumbnail_cache")
        return directory.takeIf { it.isDirectory || it.mkdirs() }?.also(::ensureNoMedia)
    }

    private fun ensureNoMedia(directory: File) {
        try {
            File(directory, ".nomedia").createNewFile()
        } catch (_: Exception) {
            // Cache still works if Android refuses the marker file.
        }
    }

    private fun createThumbnail(context: Context, item: MediaItem): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, item.uri)
            // Preserve the existing thumbnail frame choice (about 10% into the video).
            val frameTimeUs = item.duration.coerceAtLeast(0L) * 100L
            val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    frameTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    MAX_WIDTH,
                    MAX_HEIGHT
                ) ?: retriever.getFrameAtTime(
                    frameTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } else {
                retriever.getFrameAtTime(
                    frameTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } ?: return null

            scaleDown(frame)
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Ignore release failures.
            }
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_WIDTH && bitmap.height <= MAX_HEIGHT) return bitmap
        val scale = minOf(
            MAX_WIDTH.toFloat() / bitmap.width,
            MAX_HEIGHT.toFloat() / bitmap.height
        )
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun saveThumbnail(bitmap: Bitmap, destination: File) {
        val temporary = File(destination.parentFile, destination.name + ".tmp")
        try {
            FileOutputStream(temporary).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                output.flush()
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
        } catch (_: Exception) {
            temporary.delete()
            // The thumbnail still works for this display; it just won't persist.
        }
    }

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
