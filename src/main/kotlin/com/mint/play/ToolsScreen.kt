package com.mint.play

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.transformer.Transformer
import java.util.Locale
import kotlin.concurrent.thread

private enum class MediaTool { VIDEO_TO_AUDIO, TRIM_MEDIA }
private const val MIN_TRIM_MS = 500L

@Composable
fun ToolsScreen() {
    val context = LocalContext.current
    val processor = remember(context) { MediaToolProcessor(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var selectedTool by remember { mutableStateOf<MediaTool?>(null) }
    var showDownloadMedia by remember { mutableStateOf(false) }
    val downloadPreferences = remember(context) {
        context.getSharedPreferences("mintplay_download_settings", Context.MODE_PRIVATE)
    }
    var downloadVideoSegments by remember {
        mutableStateOf(downloadPreferences.getBoolean("download_video_segments", false))
    }
    var downloadLink by remember { mutableStateOf("") }
    var segmentStartTime by remember { mutableStateOf("00:00:00") }
    var segmentEndTime by remember { mutableStateOf("00:00:00") }
    var showSegmentTimeDialog by remember { mutableStateOf(false) }
    var editingSegmentStart by remember { mutableStateOf(true) }
    var selectedDownloadQuality by remember {
        mutableStateOf(downloadPreferences.getString("download_quality", null))
    }
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var sourceIsAudio by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(0L) }
    var processing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeTransformer by remember { mutableStateOf<Transformer?>(null) }

    DisposableEffect(Unit) {
        onDispose { activeTransformer?.cancel() }
    }

    val finishExport: (Result<Unit>) -> Unit = { result ->
        processing = false
        activeTransformer = null
        result.onSuccess {
            statusMessage = "Saved successfully."
            errorMessage = null
        }.onFailure { error ->
            statusMessage = null
            errorMessage = "Couldn't finish: ${error.message ?: "unsupported media or codec"}"
        }
    }

    fun startExport(
        destinationUri: Uri,
        removeVideo: Boolean,
        outputIsAudio: Boolean,
        startMs: Long? = null,
        endMs: Long? = null
    ) {
        val source = sourceUri ?: return
        processing = true
        statusMessage = "Processing media…"
        errorMessage = null
        try {
            activeTransformer = processor.export(
                sourceUri = source,
                destinationUri = destinationUri,
                removeVideo = removeVideo,
                outputIsAudio = outputIsAudio,
                startMs = startMs,
                endMs = endMs,
                onFinished = finishExport
            )
        } catch (exception: Exception) {
            processing = false
            activeTransformer = null
            statusMessage = null
            errorMessage = "Couldn't start: ${exception.message ?: "unsupported media"}"
        }
    }

    val extractOutputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { destination ->
        if (destination != null) startExport(destination, removeVideo = true, outputIsAudio = true)
    }

    val trimAudioOutputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { destination ->
        if (destination != null) {
            startExport(
                destination, removeVideo = false, outputIsAudio = true,
                startMs = trimStartMs, endMs = trimEndMs
            )
        }
    }

    val trimVideoOutputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { destination ->
        if (destination != null) {
            startExport(
                destination, removeVideo = false, outputIsAudio = false,
                startMs = trimStartMs, endMs = trimEndMs
            )
        }
    }

    val inputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val name = queryDisplayName(context, uri)
            val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val audio = mime.startsWith("audio/") ||
                extension in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
            val video = mime.startsWith("video/") ||
                extension in setOf("mp4", "mkv", "webm", "mov", "3gp", "avi", "m4v")

            if (selectedTool == MediaTool.VIDEO_TO_AUDIO && !video) {
                sourceUri = null
                sourceName = null
                durationMs = 0L
                errorMessage = "Choose a video file that contains an audio track."
            } else if (!audio && !video) {
                sourceUri = null
                sourceName = null
                durationMs = 0L
                errorMessage = "Choose a supported audio or video file."
            } else {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                sourceUri = uri
                sourceName = name
                sourceIsAudio = audio
                durationMs = 0L
                trimStartMs = 0L
                trimEndMs = 0L
                statusMessage = null
                errorMessage = null

                thread(name = "MintPlayReadDuration") {
                    val duration = runCatching {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull() ?: 0L
                        } finally {
                            retriever.release()
                        }
                    }.getOrDefault(0L)

                    mainHandler.post {
                        durationMs = duration
                        trimStartMs = 0L
                        trimEndMs = duration
                        if (duration <= 0L) {
                            errorMessage = "MintPlay couldn't read this file's duration."
                        }
                    }
                }
            }
        }
    }

    if (showSegmentTimeDialog) {
        SegmentTimeDialog(
            initialTime = if (editingSegmentStart) segmentStartTime else segmentEndTime,
            onDismiss = { showSegmentTimeDialog = false },
            onConfirm = { time ->
                if (editingSegmentStart) segmentStartTime = time else segmentEndTime = time
                showSegmentTimeDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showDownloadMedia) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "‹", color = Accent, fontSize = 30.sp,
                    modifier = Modifier.clickable { showDownloadMedia = false }
                )
                Column {
                    Text("Download Media", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Download video and audio from supported links.", color = SecondaryText, fontSize = 13.sp)
                }
            }
            SettingsSwitchRow(
                title = "Download Video Segments",
                subtitle = "Enable downloading selected sections of a video.",
                checked = downloadVideoSegments,
                onCheckedChange = { enabled ->
                    downloadVideoSegments = enabled
                    downloadPreferences.edit()
                        .putBoolean("download_video_segments", enabled)
                        .apply()
                },
                showDivider = false
            )
            run {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Video link",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = downloadLink,
                        onValueChange = { downloadLink = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = PrimaryText,
                            fontSize = 15.sp
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Accent,
                            cursorColor = Accent,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        placeholder = { Text("put video link here") }
                    )
                    Text(
                        text = if (downloadVideoSegments) {
                            "Only YouTube is supported"
                        } else {
                            "Youtube, Facebook, Instagram, Twitter(X), Tiktok links are supported"
                        },
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavBackground)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Video preview", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Video thumbnail and name will appear here", color = SecondaryText, fontSize = 13.sp)
                    }
                }

                if (downloadVideoSegments) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Put YouTube timestamp here",
                            color = PrimaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("From", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = segmentStartTime, onValueChange = {}, readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { editingSegmentStart = true; showSegmentTimeDialog = true },
                                    singleLine = true,
                                    textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Accent, unfocusedBorderColor = Accent,
                                        focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText
                                    )
                                )
                            }
                            Text("-", color = SecondaryText, modifier = Modifier.padding(top = 25.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("To", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = segmentEndTime, onValueChange = {}, readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { editingSegmentStart = false; showSegmentTimeDialog = true },
                                    singleLine = true,
                                    textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Accent, unfocusedBorderColor = Accent,
                                        focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText
                                    )
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavBackground)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Quality", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    listOf("144p" to "144p", "1080p" to "1080p", "audio" to "Download as audio").forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedDownloadQuality = value
                                downloadPreferences.edit().putString("download_quality", value).apply()
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDownloadQuality == value,
                                onClick = {
                                    selectedDownloadQuality = value
                                    downloadPreferences.edit().putString("download_quality", value).apply()
                                }
                            )
                            Text(label, color = PrimaryText, fontSize = 14.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavBackground)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Download path", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("Previously selected download folder will appear here", color = SecondaryText, fontSize = 13.sp)
                }

                Button(
                    onClick = { },
                    enabled = downloadLink.isNotBlank() && selectedDownloadQuality != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Background,
                        disabledContainerColor = NavBackground,
                        disabledContentColor = SecondaryText
                    )
                ) {
                    Text("Download", fontWeight = FontWeight.Bold)
                }
            }
        } else if (selectedTool == null) {
            Text("Tools", color = PrimaryText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Simple ways to work with your media.", color = SecondaryText, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))

            ToolCard("♫", "Video to Audio", "Extract the soundtrack from a video and save it as an audio file.") {
                selectedTool = MediaTool.VIDEO_TO_AUDIO
                sourceUri = null
                sourceName = null
                durationMs = 0L
                statusMessage = null
                errorMessage = null
            }
            ToolCard("✂", "Trim Media", "Choose a start and end point, then save just that section.") {
                selectedTool = MediaTool.TRIM_MEDIA
                sourceUri = null
                sourceName = null
                durationMs = 0L
                trimStartMs = 0L
                trimEndMs = 0L
                statusMessage = null
                errorMessage = null
            }
            ToolCard("DOWNLOAD_ICON", "Download Media", "Download video and audio from supported links.") {
                showDownloadMedia = true
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "‹", color = Accent, fontSize = 30.sp,
                    modifier = Modifier.clickable {
                        activeTransformer?.cancel()
                        activeTransformer = null
                        processing = false
                        selectedTool = null
                        sourceUri = null
                        sourceName = null
                        statusMessage = null
                        errorMessage = null
                    }
                )
                Column {
                    Text(
                        if (selectedTool == MediaTool.VIDEO_TO_AUDIO) "Video to Audio" else "Trim Media",
                        color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (selectedTool == MediaTool.VIDEO_TO_AUDIO) {
                            "Keep the sound. Leave the video behind."
                        } else "Keep only the section you need.",
                        color = SecondaryText, fontSize = 13.sp
                    )
                }
            }

            if (selectedTool == MediaTool.VIDEO_TO_AUDIO) {
                InfoPanel("Output format", "M4A audio · AAC codec")
                Text(
                    "M4A/AAC uses Android's built-in encoder, keeping MintPlay lighter than bundling a large MP3 engine.",
                    color = SecondaryText, fontSize = 12.sp
                )
            }

            if (sourceUri == null) {
                InfoPanel(
                    "Choose a file",
                    if (selectedTool == MediaTool.VIDEO_TO_AUDIO) {
                        "Select a video with an audio track."
                    } else "Select a video or audio file."
                )
            } else {
                InfoPanel(
                    sourceName ?: "Selected media",
                    if (durationMs > 0L) {
                        formatToolTime(durationMs) + if (sourceIsAudio) " · Audio" else " · Video"
                    } else "Reading file information…"
                )
            }

            if (selectedTool == MediaTool.TRIM_MEDIA && durationMs >= 1000L) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavBackground)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Start", color = SecondaryText, fontSize = 13.sp)
                        Text(formatToolTime(trimStartMs), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = trimStartMs.toFloat(),
                        onValueChange = { trimStartMs = it.toLong().coerceAtMost((trimEndMs - MIN_TRIM_MS).coerceAtLeast(0L)) },
                        valueRange = 0f..(durationMs - MIN_TRIM_MS).toFloat(),
                        enabled = !processing,
                        colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Inactive)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("End", color = SecondaryText, fontSize = 13.sp)
                        Text(formatToolTime(trimEndMs), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = trimEndMs.toFloat(),
                        onValueChange = { trimEndMs = it.toLong().coerceAtLeast(trimStartMs + MIN_TRIM_MS).coerceAtMost(durationMs) },
                        valueRange = (trimStartMs + MIN_TRIM_MS).toFloat()..durationMs.toFloat(),
                        enabled = !processing,
                        colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Inactive)
                    )
                    Text(
                        "Selected length: ${formatToolTime((trimEndMs - trimStartMs).coerceAtLeast(0L))}",
                        color = SecondaryText, fontSize = 12.sp
                    )
                }
            } else if (selectedTool == MediaTool.TRIM_MEDIA && sourceUri != null && durationMs in 1L..999L) {
                InfoPanel("File is too short", "Choose a file at least one second long to trim.")
            }

            Button(
                onClick = {
                    inputLauncher.launch(
                        if (selectedTool == MediaTool.VIDEO_TO_AUDIO) {
                            arrayOf("video/*")
                        } else arrayOf("video/*", "audio/*")
                    )
                },
                enabled = !processing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavBackground, contentColor = PrimaryText,
                    disabledContainerColor = NavBackground, disabledContentColor = SecondaryText
                )
            ) {
                Text(if (sourceUri == null) "Choose file" else "Choose another file")
            }

            Button(
                onClick = {
                    val name = sourceName ?: "MintPlay"
                    val baseName = name.substringBeforeLast('.', name).ifBlank { "MintPlay" }
                    if (selectedTool == MediaTool.VIDEO_TO_AUDIO) {
                        extractOutputLauncher.launch("${baseName}_audio.m4a")
                    } else if (sourceIsAudio) {
                        trimAudioOutputLauncher.launch("${baseName}_trimmed.m4a")
                    } else {
                        trimVideoOutputLauncher.launch("${baseName}_trimmed.mp4")
                    }
                },
                enabled = sourceUri != null && !processing &&
                    (selectedTool == MediaTool.VIDEO_TO_AUDIO ||
                        (durationMs >= 1000L && trimStartMs < trimEndMs)),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent, contentColor = Background,
                    disabledContainerColor = NavBackground, disabledContentColor = SecondaryText
                )
            ) {
                Text(
                    when {
                        processing -> "Processing…"
                        selectedTool == MediaTool.VIDEO_TO_AUDIO -> "Extract audio"
                        else -> "Save trimmed media"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (processing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        "This can take a while for large files.",
                        color = SecondaryText, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
            statusMessage?.let { Text(it, color = Accent, fontSize = 13.sp) }
            errorMessage?.let { Text(it, color = Color(0xFFFF7777), fontSize = 13.sp) }
        }
    }
}

@Composable
private fun ToolCard(
    symbol: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NavBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            if (symbol == "DOWNLOAD_ICON") {
                Canvas(modifier = Modifier.size(30.dp)) {
                    scale(size.width / 512f, size.height / 512f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                        val iconPath = Path().apply {
                            moveTo(256f, 64f)
                            lineTo(256f, 318f)
                            moveTo(160f, 226f)
                            lineTo(256f, 322f)
                            lineTo(352f, 226f)
                            moveTo(72f, 346f)
                            lineTo(72f, 400f)
                            cubicTo(72f, 422.1f, 89.9f, 440f, 112f, 440f)
                            lineTo(400f, 440f)
                            cubicTo(422.1f, 440f, 440f, 422.1f, 440f, 400f)
                            lineTo(440f, 346f)
                        }
                        drawPath(
                            path = iconPath,
                            color = Accent,
                            style = Stroke(width = 40f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            } else {
                Text(symbol, color = Accent, fontSize = 27.sp, fontWeight = FontWeight.Medium)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(description, color = SecondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("›", color = SecondaryText, fontSize = 25.sp)
    }
}

@Composable
private fun InfoPanel(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(NavBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(detail, color = SecondaryText, fontSize = 13.sp)
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull() ?: "Selected media"

private fun formatToolTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1000L
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val remainingSeconds = seconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
    }
}



@Composable
private fun SegmentTimeDialog(initialTime: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val parts = initialTime.split(":")
    var hours by remember(initialTime) { mutableStateOf(parts.getOrNull(0).orEmpty().filter(Char::isDigit).take(2)) }
    var minutes by remember(initialTime) { mutableStateOf(parts.getOrNull(1).orEmpty().filter(Char::isDigit).take(2)) }
    var seconds by remember(initialTime) { mutableStateOf(parts.getOrNull(2).orEmpty().filter(Char::isDigit).take(2)) }

    fun padded(value: String, max: Int): String =
        value.toIntOrNull()?.coerceIn(0, max)?.toString()?.padStart(2, '0') ?: "00"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavBackground,
        title = { Text("Set timestamp", color = PrimaryText, fontWeight = FontWeight.SemiBold) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Accent, cursorColor = Accent, focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText))
                    Text("h", color = SecondaryText, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Accent, cursorColor = Accent, focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText))
                    Text("m", color = SecondaryText, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(seconds, { seconds = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Accent, cursorColor = Accent, focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText))
                    Text("s", color = SecondaryText, fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm("${padded(hours, 99)}:${padded(minutes, 59)}:${padded(seconds, 59)}") }) { Text("Done", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryText) } }
    )
}
