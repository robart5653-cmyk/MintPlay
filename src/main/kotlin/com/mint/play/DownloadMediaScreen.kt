package com.mint.play

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import dev.ffmpegkit_maintained.ytdlp.DownloadProgressCallback
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

private data class DownloadQuality(val height: Int) {
    val label: String get() = "${height}p"
}

@Composable
fun DownloadMediaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("mintplay_download_settings", Context.MODE_PRIVATE)
    }
    val handler = remember { Handler(Looper.getMainLooper()) }

    var link by remember { mutableStateOf("") }
    var folderUri by remember {
        mutableStateOf(preferences.getString("download_folder_uri", null)?.let { Uri.parse(it) })
    }
    var folderName by remember {
        mutableStateOf(preferences.getString("download_folder_name", null) ?: "No folder selected")
    }
    var qualities by remember { mutableStateOf<List<DownloadQuality>>(emptyList()) }
    var selectedHeight by remember { mutableStateOf<Int?>(null) }
    var checkingQualities by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressLine by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var screenActive by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { screenActive = false }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            folderUri = uri
            folderName = readTreeName(context, uri)
            preferences.edit()
                .putString("download_folder_uri", uri.toString())
                .putString("download_folder_name", folderName)
                .apply()
        }
    }

    fun inspectQualities() {
        val url = link.trim()
        if (!isYouTubeUrl(url)) {
            errorMessage = "Enter a valid YouTube video URL."
            return
        }

        checkingQualities = true
        qualities = emptyList()
        selectedHeight = null
        status = "Checking available qualities with yt-dlp…"
        errorMessage = null

        thread(name = "MintPlayFormatCheck") {
            try {
                YtDlp.init(context.applicationContext)
                val found = inspectAvailableHeights(url)
                handler.post {
                    if (screenActive) {
                        checkingQualities = false
                        qualities = found
                        selectedHeight = found.firstOrNull()?.height
                        status = if (found.isEmpty()) {
                            "No selectable video qualities were found."
                        } else {
                            "Found ${found.size} available video quality levels."
                        }
                        errorMessage = null
                    }
                }
            } catch (exception: Exception) {
                val reason = exception.cause?.message ?: exception.message ?: "Unknown format inspection error"
                handler.post {
                    if (screenActive) {
                        checkingQualities = false
                        status = null
                        errorMessage = reason
                    }
                }
            }
        }
    }

    fun startDownload() {
        val url = link.trim()
        val destinationTree = folderUri
        val height = selectedHeight
        if (!isYouTubeUrl(url)) {
            errorMessage = "Enter a valid YouTube video URL."
            return
        }
        if (height == null) {
            errorMessage = "Check the available qualities and choose one first."
            return
        }
        if (destinationTree == null) {
            errorMessage = "Choose a download folder first."
            return
        }

        downloading = true
        progress = 0f
        progressLine = "Starting yt-dlp…"
        status = null
        errorMessage = null

        thread(name = "MintPlayDownload") {
            var taskDirectory: File? = null
            try {
                YtDlp.init(context.applicationContext)
                val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.cacheDir
                val taskDir = File(root, "mintplay_${System.currentTimeMillis()}")
                taskDirectory = taskDir
                check(taskDir.mkdirs()) { "Couldn't create the temporary download folder." }

                fun progressCallback(label: String) = DownloadProgressCallback { value, eta, line ->
                    handler.post {
                        if (screenActive) {
                            progress = value.coerceIn(0f, 100f)
                            progressLine = if (line.isNotBlank()) "$label: $line"
                            else if (eta > 0L) "$label: about $eta seconds remaining"
                            else "$label…"
                        }
                    }
                }

                // Every quality downloads separate video and audio streams, then merges them.
                // yt-dlp's own default YouTube clients are tried first; if YouTube rejects the
                // stream (HTTP 403), downloadStream() retries with a fallback client.
                handler.post { if (screenActive) progressLine = "Downloading video stream…" }
                val videoFile = downloadStream(
                    url = url,
                    taskDir = taskDir,
                    prefix = "video",
                    format = "bestvideo[height<=${height}]",
                    progress = progressCallback("Video")
                )

                handler.post { if (screenActive) progressLine = "Downloading audio stream…" }
                val audioFile = downloadStream(
                    url = url,
                    taskDir = taskDir,
                    prefix = "audio",
                    format = "bestaudio",
                    progress = progressCallback("Audio")
                )

                val merged = File(taskDir, "MintPlay_${System.currentTimeMillis()}.mkv")
                handler.post { if (screenActive) progressLine = "Merging video and audio with FFmpeg…" }

                fun quote(file: File): String = "'" + file.absolutePath.replace("'", "'\\''") + "'"
                val command = "-y -i ${quote(videoFile)} -i ${quote(audioFile)} -map 0:v:0 -map 1:a:0 -c copy ${quote(merged)}"
                val session = FFmpegKit.execute(command)
                check(ReturnCode.isSuccess(session.returnCode)) {
                    "FFmpeg couldn't merge the selected streams. Return code: ${session.returnCode}."
                }
                val output = merged

                saveFileToTree(context, destinationTree, output)
                taskDir.deleteRecursively()

                handler.post {
                    if (screenActive) {
                        downloading = false
                        progress = 100f
                        progressLine = ""
                        status = "Downloaded: ${output.name}"
                        errorMessage = null
                    }
                }
            } catch (exception: Exception) {
                taskDirectory?.deleteRecursively()
                val reason = exception.cause?.message ?: exception.message ?: "Unknown download error"
                handler.post {
                    if (screenActive) {
                        downloading = false
                        progressLine = ""
                        status = null
                        errorMessage = reason
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "‹", color = Accent, fontSize = 30.sp,
                modifier = Modifier.clickable(enabled = !downloading && !checkingQualities, onClick = onBack)
            )
            Column {
                Text("Download Media", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Choose a quality supported by the link.", color = SecondaryText, fontSize = 13.sp)
            }
        }

        Text("Video link", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = link,
            onValueChange = {
                link = it
                qualities = emptyList()
                selectedHeight = null
                status = null
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !downloading && !checkingQualities,
            singleLine = true,
            textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Accent,
                cursorColor = Accent,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText
            ),
            placeholder = { Text("Paste a YouTube video URL") }
        )

        Button(
            onClick = { inspectQualities() },
            enabled = !downloading && !checkingQualities && link.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Background,
                disabledContainerColor = NavBackground,
                disabledContentColor = SecondaryText
            )
        ) {
            Text(if (checkingQualities) "Checking qualities…" else "Check available qualities", fontWeight = FontWeight.Bold)
        }

        if (checkingQualities) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = Accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Asking yt-dlp to inspect the source…", color = SecondaryText, fontSize = 12.sp)
            }
        }

        if (qualities.isNotEmpty()) {
            Text("Available video qualities", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = !downloading) { selectedHeight = quality.height }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedHeight == quality.height,
                            onClick = { selectedHeight = quality.height },
                            enabled = !downloading,
                            colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = SecondaryText)
                        )
                        Column {
                            Text(quality.label, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Separate video + audio, merged with FFmpeg",
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavBackground)
                .clickable(enabled = !downloading) {
    folderPicker.launch(null as Uri?)
}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("Download folder", color = PrimaryText, fontWeight = FontWeight.SemiBold)
            Text(folderName, color = SecondaryText, fontSize = 13.sp)
            Text("Tap to choose a folder", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "Video and audio are downloaded as separate streams and merged into an MKV file.",
            color = SecondaryText,
            fontSize = 12.sp
        )

        Button(
            onClick = { startDownload() },
            enabled = !downloading && !checkingQualities && selectedHeight != null && folderUri != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Background,
                disabledContainerColor = NavBackground,
                disabledContentColor = SecondaryText
            )
        ) {
            Text(if (downloading) "Downloading…" else "Download selected quality", fontWeight = FontWeight.Bold)
        }

        if (downloading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Column(Modifier.weight(1f)) {
                    Text("${progress.toInt()}%", color = Accent, fontWeight = FontWeight.SemiBold)
                    Text(progressLine, color = SecondaryText, fontSize = 12.sp, maxLines = 2)
                }
            }
        }
        status?.let { Text(it, color = Accent, fontSize = 13.sp) }
        errorMessage?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFFF7777), fontSize = 13.sp) }
    }
}

private fun inspectAvailableHeights(url: String): List<DownloadQuality> {
    val python = Python.getInstance()
    val options = python.getBuiltins().callAttr("dict")
    val optionMap = options.asMap()
    optionMap[PyObject.fromJava("quiet")] = PyObject.fromJava(true)
    optionMap[PyObject.fromJava("no_warnings")] = PyObject.fromJava(true)
    optionMap[PyObject.fromJava("noplaylist")] = PyObject.fromJava(true)
    optionMap[PyObject.fromJava("skip_download")] = PyObject.fromJava(true)

    // No forced player client: let yt-dlp pick its current default YouTube clients.

    val downloader = python.getModule("yt_dlp").callAttr("YoutubeDL", options)
    val info = downloader.callAttr("extract_info", url, false)
    val formats = info.callAttr("get", "formats") ?: return emptyList()
    val heights = linkedSetOf<Int>()

    for (format in formats.asList()) {
        val height = format.callAttr("get", "height")?.toInt() ?: continue
        if (height in 144..4320) heights.add(height)
    }

    return heights.sortedDescending().map { DownloadQuality(it) }
}


// Tried in order. null = let yt-dlp choose its own default YouTube clients.
// android_vr is a fallback client that doesn't need a PO token.
private val clientAttempts: List<String?> = listOf(
    null,
    "youtube:player_client=android_vr"
)

private fun downloadStream(
    url: String,
    taskDir: File,
    prefix: String,
    format: String,
    progress: DownloadProgressCallback
): File {
    var lastExitCode = -1
    for (client in clientAttempts) {
        // Clear leftovers from a previous attempt so a half-finished file is never reused.
        taskDir.listFiles()?.filter { it.name.startsWith("$prefix.") }?.forEach { it.delete() }

        val request = YtDlpRequest(url)
            .setOutputTemplate(File(taskDir, "$prefix.%(ext)s").absolutePath)
            .addOption("--no-playlist")
            .addOption("--newline")
            .addOption("--retries", "10")
            .addOption("--fragment-retries", "10")
            .addOption("-f", format)
        if (client != null) request.addOption("--extractor-args", client)

        val response = YtDlp.executeAsync(request, progress).get()
        if (response.isSuccess()) {
            val file = taskDir.listFiles()?.firstOrNull {
                it.isFile && it.name.startsWith("$prefix.") &&
                    !it.name.endsWith(".part") && !it.name.endsWith(".ytdl")
            }
            if (file != null) return file
        }
        lastExitCode = response.exitCode
    }
    error(
        "The $prefix stream couldn't be downloaded (yt-dlp exit code $lastExitCode). " +
            "YouTube may be rejecting the request (HTTP 403). Try again later or update the app."
    )
}

private fun isYouTubeUrl(url: String): Boolean = runCatching {
    val host = Uri.parse(url).host?.lowercase(Locale.ROOT) ?: return@runCatching false
    host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ||
        host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")
}.getOrDefault(false)

private fun readTreeName(context: Context, treeUri: Uri): String = runCatching {
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri)
    )
    context.contentResolver.query(
        documentUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null, null, null
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}.getOrNull() ?: "Selected folder"

private fun saveFileToTree(context: Context, treeUri: Uri, source: File) {
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri)
    )
    val mime = when (source.extension.lowercase(Locale.ROOT)) {
        "mp4", "m4v", "mov" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/opus"
        "ogg" -> "audio/ogg"
        else -> "application/octet-stream"
    }
    val target = DocumentsContract.createDocument(
        context.contentResolver,
        documentUri,
        mime,
        source.name
    ) ?: error("Couldn't create the downloaded file in the selected folder.")

    context.contentResolver.openOutputStream(target, "w")?.use { output ->
        source.inputStream().use { input -> input.copyTo(output) }
    } ?: error("Couldn't open the destination file.")
}
