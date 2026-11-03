package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DownloadsScreen() {
    // Reading the shared task list here gives an immediate snapshot when this tab opens.
    // No polling loop or monitor runs while this composable is not on screen.
    val tasks = DownloadTaskStore.tasks

    DisposableEffect(Unit) {
        DownloadTaskStore.setMonitoringVisible(true)
        onDispose { DownloadTaskStore.setMonitoringVisible(false) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Downloads", color = PrimaryText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        if (tasks.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("No active downloads", color = SecondaryText, fontSize = 14.sp)
            Text("Start a download from the Downloader tab. Its progress will appear here.", color = SecondaryText, fontSize = 12.sp)
        } else {
            tasks.forEach { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.width(108.dp).height(68.dp)
                            .clip(RoundedCornerShape(8.dp)).background(NavBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↓", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Light)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 2, modifier = Modifier.weight(1f))
                            Text(if (task.isActive) "${task.progress.toInt()}%" else "Failed",
                                color = if (task.isActive) Accent else androidx.compose.ui.graphics.Color(0xFFFF7777),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(8.dp)).background(NavBackground)) {
                            if (task.isActive) {
                                Box(Modifier.fillMaxWidth((task.progress / 100f).coerceIn(0f, 1f))
                                    .height(5.dp).clip(RoundedCornerShape(8.dp)).background(Accent))
                            }
                        }
                        Text(task.eta ?: if (task.isActive) "Time remaining: calculating…" else (task.error ?: ""),
                            color = SecondaryText, fontSize = 11.sp, maxLines = 1)
                        if (task.totalBytes > 0L) {
                            Text("~${formatDownloadBytes(task.downloadedBytes)} / ${formatDownloadBytes(task.totalBytes)} (estimated)",
                                color = PrimaryText, fontSize = 11.sp)
                        } else {
                            Text("Size: calculating…", color = SecondaryText, fontSize = 11.sp)
                        }
                        Text(task.status, color = SecondaryText, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
