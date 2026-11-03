package com.mint.play

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch

/** A time range inside a video, in whole seconds. */
internal class ClipRange(val startSeconds: Int, val endSeconds: Int)

/** Everything yt-dlp told us about a video. Fetched once and reused by the downloads. */
internal class VideoInfoData(
    val url: String,
    val title: String,
    val durationSeconds: Int,
    val heights: List<Int>,
    val infoFile: File,
    val json: JSONObject,
    val clientIndex: Int,
    val fetchedAt: Long = System.currentTimeMillis()
)

/**
 * All yt-dlp work lives here. Every function blocks, so call them from a background thread.
 *
 * Built on youtubedl-android, which can update its bundled yt-dlp at runtime and ships the
 * JavaScript runtime that YouTube's challenges need.
 *
 * The slow part of yt-dlp (extracting the video and solving YouTube's JS challenge) runs once
 * in [fetchInfo]. The result is saved as an info.json file, and the stream downloads reuse it
 * with --load-info-json instead of extracting again.
 */
internal object YoutubeDownloader {
    // Stream URLs expire after a few hours, so old info is fetched again.
    private const val INFO_MAX_AGE_MS = 4L * 60 * 60 * 1000

    private val lock = Any()
    private var initialized = false
    private var updated = false
    private var cacheDirPath: String? = null

    // Tried in order. null = let yt-dlp choose its own default YouTube clients.
    // android_vr is a fallback client that doesn't need a PO token.
    private val clientAttempts: List<String?> = listOf(
        null,
        "youtube:player_client=android_vr"
    )

    /** Starts yt-dlp and updates it to the newest version. Safe to call repeatedly. */
    fun prepare(context: Context, onStatus: (String) -> Unit = {}) {
        synchronized(lock) {
            val appContext = context.applicationContext
            if (cacheDirPath == null) {
                // A persistent cache lets yt-dlp reuse work (like solved player code) between runs.
                cacheDirPath = File(appContext.noBackupFilesDir, "yt-dlp-cache")
                    .apply { mkdirs() }.absolutePath
            }
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

    /**
     * The slow step: asks yt-dlp about the video (title, length, formats). The result is saved
     * to disk so the downloads can skip extraction entirely.
     */
    fun fetchInfo(
        context: Context,
        url: String,
        onStatus: (String) -> Unit = {},
        startClient: Int = 0
    ): VideoInfoData {
        prepare(context, onStatus)
        onStatus("Checking available qualities with yt-dlp…")

        val infoDir = File(context.applicationContext.cacheDir, "mintplay-info").apply { mkdirs() }
        var lastError: Exception? = null
        for (index in startClient until clientAttempts.size) {
            try {
                val request = clientArgs(commonOptions(YoutubeDLRequest(url)), clientAttempts[index])
                    .addOption("--dump-json")
                val response = YoutubeDL.getInstance().execute(request, null, false, null)
                val line = response.out.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("{") }
                    ?: throw IllegalStateException("yt-dlp returned no video information.")
                val json = JSONObject(line)
                val heights = heightsOf(json)
                if (heights.isEmpty()) {
                    throw IllegalStateException("No selectable video qualities were found.")
                }
                val infoFile = File(infoDir, "info.json")
                infoFile.writeText(line)
                return VideoInfoData(
                    url = url,
                    title = json.text("title") ?: json.text("id") ?: "video",
                    durationSeconds = json.optInt("duration", 0),
                    heights = heights,
                    infoFile = infoFile,
                    json = json,
                    clientIndex = index
                )
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Couldn't read the video information.")
    }

    /**
     * Downloads the video (or only [clip] of it) in quality up to [height] and returns the
     * finished .mkv, named after the video title. Pass the [cached] info from [fetchInfo]
     * to skip the slow extraction.
     */
    fun download(
        context: Context,
        url: String,
        height: Int,
        workDir: File,
        cachedInfo: VideoInfoData?,
        clip: ClipRange?,
        onStatus: (String) -> Unit,
        onProgress: (percent: Float, line: String) -> Unit
    ): File {
        prepare(context, onStatus)

        var info = cachedInfo?.takeIf {
            it.url == url && System.currentTimeMillis() - it.fetchedAt < INFO_MAX_AGE_MS
        } ?: fetchInfo(context, url, onStatus)

        while (true) {
            try {
                return if (clip == null) {
                    downloadFull(info, height, workDir, onStatus, onProgress)
                } else {
                    downloadClip(info, height, clip, workDir, onStatus, onProgress)
                }
            } catch (e: Exception) {
                // YouTube sometimes rejects one client's streams (HTTP 403). Fetch the info
                // again through the next client and retry; give up when none are left.
                val next = info.clientIndex + 1
                if (next >= clientAttempts.size) throw e
                onStatus("That attempt failed, retrying with another YouTube client…")
                info = try {
                    fetchInfo(context, url, onStatus, next)
                } catch (ignored: Exception) {
                    throw e
                }
            }
        }
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

    // ---------------------------------------------------------------- full download

    private fun downloadFull(
        info: VideoInfoData,
        height: Int,
        workDir: File,
        onStatus: (String) -> Unit,
        onProgress: (Float, String) -> Unit
    ): File {
        onStatus("Downloading video stream…")
        val video = downloadStream(info, workDir, "video", "bestvideo[height<=$height]") { percent, line ->
            onProgress(percent, label("Video", line))
        }

        onStatus("Downloading audio stream…")
        val audio = downloadStream(info, workDir, "audio", "bestaudio") { percent, line ->
            onProgress(percent, label("Audio", line))
        }

        onStatus("Merging video and audio with FFmpeg…")
        val output = File(workDir, safeFileName(info.title, info.json.text("id") ?: "video") + ".mkv")
        return merge(video, audio, output)
    }

    private fun downloadStream(
        info: VideoInfoData,
        workDir: File,
        prefix: String,
        format: String,
        onProgress: (Float, String) -> Unit
    ): File {
        // Clear leftovers so a half-finished file from an earlier attempt is never reused.
        workDir.listFiles()?.filter { it.name.startsWith("$prefix.") }?.forEach { it.delete() }

        // No URL here: --load-info-json makes yt-dlp skip extraction and use the saved info.
        val request = commonOptions(YoutubeDLRequest(emptyList<String>()))
            .addOption("--load-info-json", info.infoFile.absolutePath)
            .addOption("--newline")
            .addOption("-o", File(workDir, "$prefix.%(ext)s").absolutePath)
            .addOption("-f", format)

        val callback: (Float, Long, String) -> Unit = { percent, _, line -> onProgress(percent, line) }
        YoutubeDL.getInstance().execute(request, null, false, callback)

        return workDir.listFiles()?.firstOrNull {
            it.isFile && it.name.startsWith("$prefix.") &&
                !it.name.endsWith(".part") && !it.name.endsWith(".ytdl")
        } ?: throw IllegalStateException("yt-dlp finished but no $prefix file was created.")
    }

    private fun merge(video: File, audio: File, output: File): File {
        val session = FFmpegKit.executeWithArguments(
            arrayOf(
                "-y", "-i", video.absolutePath, "-i", audio.absolutePath,
                "-map", "0:v:0", "-map", "1:a:0", "-c", "copy", output.absolutePath
            )
        )
        check(ReturnCode.isSuccess(session.returnCode)) {
            "FFmpeg couldn't merge the video and audio streams (return code ${session.returnCode})."
        }
        return output
    }

    // ---------------------------------------------------------------- segment download

    /**
     * Downloads only [clip]. yt-dlp's own --download-sections needs a separate ffmpeg binary,
     * so instead the direct stream URLs from the saved info go straight to FFmpegKit, which
     * seeks to the start time and reads just that part of each stream.
     */
    private fun downloadClip(
        info: VideoInfoData,
        height: Int,
        clip: ClipRange,
        workDir: File,
        onStatus: (String) -> Unit,
        onProgress: (Float, String) -> Unit
    ): File {
        val video = pickVideoFormat(info.json, height)
            ?: throw IllegalStateException("No directly downloadable video stream was found for ${height}p.")
        val audio = pickAudioFormat(info.json)
            ?: throw IllegalStateException("No directly downloadable audio stream was found.")

        val length = clip.endSeconds - clip.startSeconds
        val name = safeFileName(info.title, info.json.text("id") ?: "video") +
            " (${stamp(clip.startSeconds)} to ${stamp(clip.endSeconds)}).mkv"
        val output = File(workDir, name)

        onStatus("Downloading ${clock(clip.startSeconds)} to ${clock(clip.endSeconds)} with FFmpeg…")

        val args = ArrayList<String>()
        args += listOf("-y", "-hide_banner", "-nostdin")
        for (format in listOf(video, audio)) {
            val headers = headerBlock(format, info.json)
            if (headers.isNotEmpty()) args += listOf("-headers", headers)
            args += listOf(
                "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5",
                "-ss", clip.startSeconds.toString(),
                "-i", format.getString("url")
            )
        }
        args += listOf(
            "-t", length.toString(),
            "-map", "0:v:0", "-map", "1:a:0",
            "-c", "copy",
            "-avoid_negative_ts", "make_zero",
            output.absolutePath
        )

        val totalMs = length * 1000.0
        val done = CountDownLatch(1)
        val session = FFmpegKit.executeWithArgumentsAsync(
            args.toTypedArray(),
            FFmpegSessionCompleteCallback { done.countDown() },
            null,
            StatisticsCallback { statistics ->
                val percent = (statistics.time.toDouble() / totalMs * 100.0).toFloat().coerceIn(0f, 100f)
                onProgress(percent, "Segment: ${percent.toInt()}%")
            }
        )
        done.await()

        if (!ReturnCode.isSuccess(session.returnCode) || !output.exists() || output.length() == 0L) {
            throw IllegalStateException(
                "FFmpeg couldn't download the segment: " + lastLogLines(session.allLogsAsString)
            )
        }
        return output
    }

    private fun pickVideoFormat(json: JSONObject, maxHeight: Int): JSONObject? =
        formatList(json)
            .filter { f ->
                val v = f.text("vcodec")
                val a = f.text("acodec")
                v != null && v != "none" && (a == null || a == "none") &&
                    f.optInt("height", 0) in 1..maxHeight && isDirect(f)
            }
            .sortedWith(
                compareByDescending<JSONObject> { it.optInt("height", 0) }
                    .thenByDescending { it.optDouble("fps", 0.0) }
                    .thenBy { codecRank(it.text("vcodec")) }
                    .thenByDescending { it.optDouble("tbr", 0.0) }
            )
            .firstOrNull()

    private fun pickAudioFormat(json: JSONObject): JSONObject? {
        val candidates = formatList(json).filter { f ->
            val v = f.text("vcodec")
            val a = f.text("acodec")
            (v == null || v == "none") && a != null && a != "none" && isDirect(f)
        }
        val withoutDrc = candidates.filterNot { (it.text("format_id") ?: "").contains("drc", ignoreCase = true) }
        return withoutDrc.ifEmpty { candidates }
            .sortedWith(
                compareByDescending<JSONObject> { it.optInt("language_preference", -1) }
                    .thenByDescending { maxOf(it.optDouble("abr", 0.0), it.optDouble("tbr", 0.0)) }
            )
            .firstOrNull()
    }

    // H.264 first: it plays on every phone. Then VP9, then AV1.
    private fun codecRank(codec: String?): Int = when {
        codec == null -> 3
        codec.startsWith("avc1") || codec.startsWith("h264") -> 0
        codec.startsWith("vp09") || codec.startsWith("vp9") -> 1
        codec.startsWith("av01") -> 2
        else -> 3
    }

    private fun isDirect(format: JSONObject): Boolean {
        val url = format.text("url") ?: return false
        val protocol = format.text("protocol") ?: "https"
        return url.startsWith("http") && (protocol == "https" || protocol == "http")
    }

    private fun headerBlock(format: JSONObject, root: JSONObject): String {
        val headers = format.optJSONObject("http_headers") ?: root.optJSONObject("http_headers") ?: return ""
        val block = StringBuilder()
        val keys = headers.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = headers.optString(key)
            // FFmpeg can't decompress responses, so never ask for them.
            if (value.isBlank() || key.equals("Accept-Encoding", ignoreCase = true)) continue
            block.append(key).append(": ").append(value).append("\r\n")
        }
        return block.toString()
    }

    private fun lastLogLines(logs: String?): String {
        val lines = (logs ?: "").lines()
            .map { it.trim().replace(Regex("https?://\\S+"), "<url>") }
            .filter { it.isNotEmpty() }
        return lines.takeLast(2).joinToString(" ").take(240).ifEmpty { "unknown error" }
    }

    // ---------------------------------------------------------------- helpers

    private fun commonOptions(request: YoutubeDLRequest): YoutubeDLRequest {
        request.addOption("--no-playlist")
            .addOption("--socket-timeout", 30)
            .addOption("--retries", 10)
            .addOption("--fragment-retries", 10)
        // Without this the library passes --no-cache-dir, and yt-dlp redoes work every time.
        cacheDirPath?.let { request.addOption("--cache-dir", it) }
        return request
    }

    private fun clientArgs(request: YoutubeDLRequest, client: String?): YoutubeDLRequest {
        if (client != null) request.addOption("--extractor-args", client)
        return request
    }

    private fun formatList(json: JSONObject): List<JSONObject> {
        val formats = json.optJSONArray("formats") ?: return emptyList()
        return (0 until formats.length()).mapNotNull { formats.optJSONObject(it) }
    }

    private fun heightsOf(json: JSONObject): List<Int> {
        val heights = sortedSetOf<Int>()
        for (format in formatList(json)) {
            val vcodec = format.text("vcodec")
            if (vcodec == null || vcodec == "none") continue
            val height = format.optInt("height", 0)
            if (height in 144..4320) heights.add(height)
        }
        return heights.sortedDescending()
    }

    // JSON null and missing keys both become Kotlin null (org.json would give the text "null").
    private fun JSONObject.text(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun label(name: String, line: String) = if (line.isBlank()) "$name…" else "$name: $line"

    // Titles can hold characters that are illegal in file names; this keeps the rest as is.
    private fun safeFileName(title: String, fallback: String): String {
        val cleaned = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(100)
            .trim()
        return cleaned.ifEmpty { fallback }
    }

    private fun stamp(seconds: Int) =
        String.format(Locale.ROOT, "%02d-%02d-%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)

    private fun clock(seconds: Int) =
        String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}
