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
import dev.ffmpegkit_maintained.ytdlp.DownloadProgressCallback
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

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

    fun startDownload() {
        val url = link.trim()
        val destinationTree = folderUri
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            errorMessage = "Enter a valid video URL."
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

                val outputTemplate = File(
                    taskDir,
                    "%(title).150B [%(id)s].%(ext)s"
                ).absolutePath
                val request = YtDlpRequest(url)
                    .setOutputTemplate(outputTemplate)
                    .addOption("--no-playlist")
                    .addOption("--newline")
                    .addOption("-f", "best")

                val callback = DownloadProgressCallback { value, eta, line ->
                    handler.post {
                        if (screenActive) {
                            progress = value.coerceIn(0f, 100f)
                            progressLine = line.ifBlank {
                                if (eta > 0L) "About $eta seconds remaining" else "Downloading…"
                            }
                        }
                    }
                }

                val response = YtDlp.executeAsync(request, callback).get()
                check(response.isSuccess()) { "yt-dlp exited with code ${response.exitCode}." }

                val output = taskDir.listFiles()
                    ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                    ?.maxByOrNull { it.lastModified() }
                    ?: throw IllegalStateException("yt-dlp finished but no output file was found.")

                saveFileToTree(context, destinationTree, output)
                output.delete()
                taskDir.delete()

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
                modifier = Modifier.clickable(enabled = !downloading, onClick = onBack)
            )
            Column {
                Text("Download Media", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Download video and audio from a link.", color = SecondaryText, fontSize = 13.sp)
            }
        }

        Text("Video link", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !downloading,
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
            placeholder = { Text("Paste a video URL") }
        )

        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavBackground)
                .clickable(enabled = !downloading) { folderPicker.launch(null) }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("Download folder", color = PrimaryText, fontWeight = FontWeight.SemiBold)
            Text(folderName, color = SecondaryText, fontSize = 13.sp)
            Text("Tap to choose a folder", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "Downloads use the best single-file format available. Higher resolutions that require separate video/audio merging will be added with FFmpeg support.",
            color = SecondaryText,
            fontSize = 12.sp
        )
        Text(
            "YouTube time-range downloads are not enabled yet; they need FFmpeg for cutting and merging.",
            color = SecondaryText,
            fontSize = 12.sp
        )

        Button(
            onClick = { startDownload() },
            enabled = !downloading && link.isNotBlank() && folderUri != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Background,
                disabledContainerColor = NavBackground,
                disabledContentColor = SecondaryText
            )
        ) {
            Text(if (downloading) "Downloading…" else "Download", fontWeight = FontWeight.Bold)
        }

        if (downloading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = Accent,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
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
