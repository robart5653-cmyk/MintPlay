package com.mint.play

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/**
 * All yt-dlp work lives here. Every function blocks, so call them from a background thread.
 *
 * Built on youtubedl-android, which (unlike the old yt-dlp-android) can update its bundled
 * yt-dlp at runtime and ships the JavaScript runtime that YouTube's challenges need.
 * Video and audio are fetched as separate streams and merged into an MKV with FFmpegKit.
 */
internal object YoutubeDownloader {
    private val lock = Any()
    private var initialized = false
    private var updated = false

    // Tried in order. null = let yt-dlp choose its own default YouTube clients.
    private val clientAttempts: List<String?> = listOf(
        null,
        "youtube:player_client=android_vr"
    )

    /** Starts yt-dlp and updates it to the newest version. Safe to call repeatedly. */
    fun prepare(context: Context, onStatus: (String) -> Unit = {}) {
        synchronized(lock) {
            val appContext = context.applicationContext
            if (!initialized) {
                onStatus("Starting yt-dlp…")
                YoutubeDL.getInstance().init(appContext)
                initialized = true
            }
            if (!updated) {
                onStatus("Updating yt-dlp…")
                updated = updateYtDlp(appContext)
            }
        }
    }

    // YouTube breaks yt-dlp often, and the nightly channel gets fixes first.
    // Falls back to stable, then to whatever version is already installed (e.g. offline).
    private fun updateYtDlp(appContext: Context): Boolean {
        val channels = listOf(YoutubeDL.UpdateChannel.NIGHTLY, YoutubeDL.UpdateChannel.STABLE)
        for (channel in channels) {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(appContext, channel)
                return true
            } catch (ignored: Exception) {
            }
        }
        return false
    }

    /** Heights (e.g. 1080, 720, 360) that have a video stream, best first. */
    fun availableHeights(
        context: Context,
        url: String,
        onStatus: (String) -> Unit = {}
    ): List<Int> {
        prepare(context, onStatus)
        onStatus("Checking available qualities with yt-dlp…")

        var lastError: Exception? = null
        for (client in clientAttempts) {
            try {
                val info = YoutubeDL.getInstance().getInfo(baseRequest(url, client))
                val heights = info.formats.orEmpty()
                    .filter { it.vcodec != null && it.vcodec != "none" }
                    .map { it.height }
                    .filter { it in 144..4320 }
                    .distinct()
                    .sortedDescending()
                if (heights.isNotEmpty()) return heights
            } catch (e: Exception) {
                lastError = e
            }
        }
        lastError?.let { throw it }
        return emptyList()
    }

    /** Downloads video + audio up to [height], merges them, and returns the merged file. */
    fun download(
        context: Context,
        url: String,
        height: Int,
        workDir: File,
        onStatus: (String) -> Unit,
        onProgress: (percent: Float, line: String) -> Unit
    ): File {
        prepare(context, onStatus)

        onStatus("Downloading video stream…")
        val video = downloadStream(url, workDir, "video", "bestvideo[height<=$height]") { percent, line ->
            onProgress(percent, label("Video", line))
        }

        onStatus("Downloading audio stream…")
        val audio = downloadStream(url, workDir, "audio", "bestaudio") { percent, line ->
            onProgress(percent, label("Audio", line))
        }

        onStatus("Merging video and audio with FFmpeg…")
        return merge(video, audio, File(workDir, "MintPlay_${System.currentTimeMillis()}.mkv"))
    }

    /** Short, readable text for an exception thrown by yt-dlp or this class. */
    fun describe(error: Throwable): String {
        val raw = error.message?.takeIf { it.isNotBlank() }
            ?: error.cause?.message
            ?: error.javaClass.simpleName
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val main = lines.lastOrNull { it.startsWith("ERROR", ignoreCase = true) }
            ?: lines.lastOrNull()
            ?: "Unknown yt-dlp error"
        return when {
            raw.contains("Cannot run program") || raw.contains("libpython") ->
                "$main (yt-dlp couldn't start: check that android:extractNativeLibs=\"true\" is set in the manifest.)"
            raw.contains("403") ->
                "$main (YouTube rejected the stream. Restart the app so yt-dlp can update, or try again later.)"
            else -> main
        }
    }

    private fun label(name: String, line: String) = if (line.isBlank()) "$name…" else "$name: $line"

    private fun baseRequest(url: String, client: String?): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
            .addOption("--no-playlist")
            .addOption("--socket-timeout", 30)
            .addOption("--retries", 10)
            .addOption("--fragment-retries", 10)
        if (client != null) request.addOption("--extractor-args", client)
        return request
    }

    private fun downloadStream(
        url: String,
        workDir: File,
        prefix: String,
        format: String,
        onProgress: (Float, String) -> Unit
    ): File {
        var lastError: Exception? = null
        for (client in clientAttempts) {
            // Clear leftovers so a half-finished file from another attempt is never reused.
            workDir.listFiles()?.filter { it.name.startsWith("$prefix.") }?.forEach { it.delete() }

            val request = baseRequest(url, client)
                .addOption("--newline")
                .addOption("-o", File(workDir, "$prefix.%(ext)s").absolutePath)
                .addOption("-f", format)

            try {
                val callback: (Float, Long, String) -> Unit = { percent, _, line ->
                    onProgress(percent, line)
                }
                YoutubeDL.getInstance().execute(request, null, false, callback)

                val file = workDir.listFiles()?.firstOrNull {
                    it.isFile && it.name.startsWith("$prefix.") &&
                        !it.name.endsWith(".part") && !it.name.endsWith(".ytdl")
                }
                if (file != null) return file
                lastError = IllegalStateException("yt-dlp finished but no $prefix file was created.")
            } catch (e: Exception) {
                // yt-dlp reports failures (including HTTP 403) by throwing; try the next client.
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("The $prefix stream couldn't be downloaded.")
    }

    private fun merge(video: File, audio: File, output: File): File {
        fun quote(file: File): String = "'" + file.absolutePath.replace("'", "'\\''") + "'"
        val command =
            "-y -i ${quote(video)} -i ${quote(audio)} -map 0:v:0 -map 1:a:0 -c copy ${quote(output)}"
        val session = FFmpegKit.execute(command)
        check(ReturnCode.isSuccess(session.returnCode)) {
            "FFmpeg couldn't merge the video and audio streams (return code ${session.returnCode})."
        }
        return output
    }
}
