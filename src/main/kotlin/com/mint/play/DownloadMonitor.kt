package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal object DownloadMonitorState {
    var title by mutableStateOf("")
        private set
    var progress by mutableStateOf(0f)
        private set
    var downloadedBytes by mutableStateOf<Long?>(null)
        private set
    var totalBytes by mutableStateOf<Long?>(null)
        private set
    var etaSeconds by mutableStateOf<Long?>(null)
        private set
    var status by mutableStateOf("")
        private set

    private val sizePattern = Regex("""(?i)\bof\s+([0-9]+(?:\.[0-9]+)?)\s*(B|KiB|MiB|GiB|KB|MB|GB)\b""")
    private val etaPattern = Regex("""(?i)\bETA\s+((?:[0-9]+:)?[0-9]{1,2}:[0-9]{2})""")

    fun begin(videoTitle: String) {
        title = videoTitle
        progress = 0f
        downloadedBytes = null
        totalBytes = null
        etaSeconds = null
        status = "Starting download…"
    }

    fun updateStatus(message: String) {
        status = when {
            message.contains("Downloading video stream", true) -> "Downloading video"
            message.contains("Downloading audio stream", true) -> "Downloading audio"
            message.contains("with FFmpeg", true) -> "Downloading segment"
            message.contains("Merging", true) -> "Merging streams"
            else -> message
        }
        if (status == "Downloading video" || status == "Downloading audio" || status == "Downloading segment") {
            progress = 0f
            downloadedBytes = null
            totalBytes = null
            etaSeconds = null
        }
    }

    fun updateProgress(value: Float, line: String) {
        progress = value.coerceIn(0f, 100f)
        sizePattern.find(line)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val multiplier = when (unit) {
                "b" -> 1.0
                "kib" -> 1024.0
                "mib" -> 1024.0 * 1024.0
                "gib" -> 1024.0 * 1024.0 * 1024.0
                "kb" -> 1000.0
                "mb" -> 1000.0 * 1000.0
                "gb" -> 1000.0 * 1000.0 * 1000.0
                else -> 1.0
            }
            totalBytes = (amount * multiplier).toLong()
        }
        totalBytes?.let { downloadedBytes = (it * progress / 100f).toLong() }
        etaPattern.find(line)?.groupValues?.get(1)?.let { eta ->
            val parts = eta.split(":").mapNotNull { it.toLongOrNull() }
            etaSeconds = when (parts.size) {
                2 -> parts[0] * 60 + parts[1]
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                else -> null
            }
        }
    }

    fun complete() {
        progress = 100f
        downloadedBytes = totalBytes
        etaSeconds = 0L
        status = "Download complete"
    }

    fun fail(message: String) {
        status = "Download failed: $message"
        etaSeconds = null
    }
}

@Composable
internal fun DownloadMonitorCard() {
    val title = DownloadMonitorState.title
    val progress = DownloadMonitorState.progress
    val downloaded = DownloadMonitorState.downloadedBytes
    val total = DownloadMonitorState.totalBytes
    val eta = DownloadMonitorState.etaSeconds
    val status = DownloadMonitorState.status

    if (title.isBlank()) {
        Text(
            "No downloads yet",
            color = SecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(NavBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(title, color = PrimaryText, fontSize = 15.sp, maxLines = 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((progress / 100f).coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Accent)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${progress.toInt()}%", color = Accent, fontSize = 12.sp)
            val details = buildString {
                if (downloaded != null && total != null && total > 0L) {
                    append("${formatMegabytes(downloaded)} / ${formatMegabytes(total)} MB")
                }
                if (eta != null) {
                    if (isNotEmpty()) append("  •  ")
                    append("${formatEta(eta)} left")
                }
            }
            if (details.isNotEmpty()) Text(details, color = SecondaryText, fontSize = 12.sp)
        }
        if (status.isNotBlank()) Text(status, color = SecondaryText, fontSize = 11.sp, maxLines = 1)
    }
}

private fun formatMegabytes(bytes: Long): String =
    String.format(Locale.US, "%.0f", bytes / 1_000_000.0)

private fun formatEta(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val minutes = safe / 60
    val remainingSeconds = safe % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}
