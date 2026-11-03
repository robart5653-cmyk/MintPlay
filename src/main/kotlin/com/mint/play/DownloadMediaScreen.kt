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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
    var videoInfo by remember { mutableStateOf<VideoInfoData?>(null) }
    var clipEnabled by remember { mutableStateOf(false) }
    var clipStart by remember { mutableStateOf("") }
    var clipEnd by remember { mutableStateOf("") }
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

    // Start and update yt-dlp in the background while the user pastes a link.
    LaunchedEffect(Unit) {
        thread(name = "MintPlayPrepare") {
            runCatching { YoutubeDownloader.prepare(context.applicationContext) }
        }
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
        videoInfo = null
        selectedHeight = null
        status = "Checking available qualities with yt-dlp…"
        errorMessage = null

        thread(name = "MintPlayFormatCheck") {
            try {
                val info = YoutubeDownloader.fetchInfo(context.applicationContext, url, { message ->
                    handler.post { if (screenActive) status = message }
                })
                val found = info.heights.map { DownloadQuality(it) }
                handler.post {
                    if (screenActive) {
                        checkingQualities = false
                        videoInfo = info
                        qualities = found
                        selectedHeight = found.firstOrNull()?.height
                        status = "Found ${found.size} available video quality levels."
                        errorMessage = null
                    }
                }
            } catch (exception: Exception) {
                val reason = YoutubeDownloader.describe(exception)
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

        var clip: ClipRange? = null
        if (clipEnabled) {
            val start = parseTimestamp(clipStart)
            val end = parseTimestamp(clipEnd)
            val length = videoInfo?.durationSeconds ?: 0
            if (start == null || end == null) {
                errorMessage = "Enter the start and end times like 2:10:20."
                return
            }
            if (end <= start) {
                errorMessage = "The end time must be after the start time."
                return
            }
            if (length > 0 && start >= length) {
                errorMessage = "The start time is past the end of the video (${formatDuration(length)})."
                return
            }
            clip = ClipRange(start, if (length > 0) minOf(end, length) else end)
        }
        val cachedInfo = videoInfo

        downloading = true
        DownloadMonitorState.begin(cachedInfo?.title ?: "YouTube video")
        progress = 0f
        progressLine = "Starting yt-dlp…"
        status = null
        errorMessage = null

        thread(name = "MintPlayDownload") {
            var taskDirectory: File? = null
            try {
                val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.cacheDir
                val taskDir = File(root, "mintplay_${System.currentTimeMillis()}")
                taskDirectory = taskDir
                check(taskDir.mkdirs()) { "Couldn't create the temporary download folder." }

                val output = YoutubeDownloader.download(
                    context = context.applicationContext,
                    url = url,
                    height = height,
                    workDir = taskDir,
                    cachedInfo = cachedInfo,
                    clip = clip,
                    onStatus = { message ->
                        handler.post {
                            DownloadMonitorState.updateStatus(message)
                            if (screenActive) progressLine = message
                        }
                    },
                    onProgress = { percent, line ->
                        handler.post {
                            DownloadMonitorState.updateProgress(percent, line)
                            if (screenActive) {
                                progress = percent.coerceIn(0f, 100f)
                                progressLine = line
                            }
                        }
                    }
                )

                saveFileToTree(context, destinationTree, output)
                taskDir.deleteRecursively()

                handler.post {
                    DownloadMonitorState.complete()
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
                val reason = YoutubeDownloader.describe(exception)
                handler.post {
                    DownloadMonitorState.fail(reason)
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
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 2.dp)
                .clickable(enabled = !downloading && !checkingQualities, onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("‹", color = Accent, fontSize = 30.sp)
            Text("Downloads", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Text("Video link", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = link,
            onValueChange = {
                link = it
                qualities = emptyList()
                videoInfo = null
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
            videoInfo?.let { info ->
                Text(info.title, color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                if (info.durationSeconds > 0) {
                    Text("Length ${formatDuration(info.durationSeconds)}", color = SecondaryText, fontSize = 12.sp)
                }
                Text(info.summary, color = SecondaryText, fontSize = 10.sp)
            }
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

        if (qualities.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Switch(
                    checked = clipEnabled,
                    onCheckedChange = { clipEnabled = it },
                    enabled = !downloading,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Background,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = SecondaryText,
                        uncheckedTrackColor = NavBackground,
                        uncheckedBorderColor = SecondaryText
                    )
                )
                Column {
                    Text("Download only a segment", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Only the chosen part is downloaded. Cuts snap to the nearest keyframe.",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }
            if (clipEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeField("Start (0:00:00)", clipStart, !downloading, Modifier.weight(1f)) { clipStart = it }
                    TimeField("End (0:10:00)", clipEnd, !downloading, Modifier.weight(1f)) { clipEnd = it }
                }
                Text(
                    "Use H:MM:SS, MM:SS or plain seconds, for example 2:10:20.",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
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
            "Video and audio are downloaded as separate streams and merged into an MKV file named after the video title.",
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
            Text(
                if (downloading) "Downloading…" else if (clipEnabled) "Download segment" else "Download selected quality",
                fontWeight = FontWeight.Bold
            )
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

@Composable
private fun TimeField(
    label: String,
    value: String,
    enabled: Boolean,
    modifier: Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        enabled = enabled,
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
        placeholder = { Text(label) }
    )
}

// Accepts H:MM:SS, MM:SS or plain seconds. Returns null for anything else.
private fun parseTimestamp(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.isEmpty() || parts.size > 3) return null
    var total = 0L
    for (part in parts) {
        val value = part.trim().toLongOrNull() ?: return null
        if (value < 0) return null
        total = total * 60 + value
    }
    return if (total <= 48L * 3600) total.toInt() else null
}

private fun formatDuration(seconds: Int): String =
    String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
