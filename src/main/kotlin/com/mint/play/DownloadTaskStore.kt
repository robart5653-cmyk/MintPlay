package com.mint.play

import androidx.compose.runtime.mutableStateListOf

internal data class DownloadTask(
    val id: String,
    val title: String,
    val progress: Float = 0f,
    val status: String = "Starting…",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val eta: String? = null,
    val rawProgressLine: String = "",
    val isActive: Boolean = true,
    val error: String? = null
)

internal object DownloadTaskStore {
    val tasks = mutableStateListOf<DownloadTask>()
    private var monitoringVisible = false

    fun add(task: DownloadTask) { tasks.add(task) }

    fun updateStatus(id: String, message: String) = update(id) { it.copy(status = message) }

    // While Downloads is hidden, retain only the latest raw callback. Do not parse sizes or ETA.
    fun setMonitoringVisible(visible: Boolean) {
        monitoringVisible = visible
        if (visible) {
            for (index in tasks.indices) tasks[index] = withMetrics(tasks[index])
        }
    }

    fun updateProgress(id: String, percent: Float, line: String) = update(id) { task ->
        val latest = task.copy(progress = percent.coerceIn(0f, 100f), status = line, rawProgressLine = line)
        if (monitoringVisible) withMetrics(latest) else latest
    }

    private fun withMetrics(task: DownloadTask): DownloadTask {
        if (task.rawProgressLine.isBlank()) return task
        val total = parseTotalBytes(task.rawProgressLine).takeIf { it > 0L } ?: task.totalBytes
        val downloaded = if (total > 0L) (total * task.progress / 100f).toLong() else task.downloadedBytes
        return task.copy(
            downloadedBytes = downloaded,
            totalBytes = total,
            eta = parseEta(task.rawProgressLine) ?: task.eta
        )
    }

    fun complete(id: String) { tasks.removeAll { it.id == id } }

    fun fail(id: String, message: String) = update(id) {
        it.copy(isActive = false, status = "Download failed", error = message, eta = null)
    }

    private fun update(id: String, transform: (DownloadTask) -> DownloadTask) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index >= 0) tasks[index] = transform(tasks[index])
    }

    private fun parseTotalBytes(line: String): Long {
        val match = Regex("(?i)\\bof\\s+~?\\s*([0-9.]+)\\s*([kmgt]?i?b)\\b").find(line) ?: return 0L
        val amount = match.groupValues[1].toDoubleOrNull() ?: return 0L
        val unit = match.groupValues[2].lowercase()
        val multiplier = when (unit) {
            "kb" -> 1_000.0; "kib" -> 1_024.0
            "mb" -> 1_000_000.0; "mib" -> 1_048_576.0
            "gb" -> 1_000_000_000.0; "gib" -> 1_073_741_824.0
            "tb" -> 1_000_000_000_000.0; "tib" -> 1_099_511_627_776.0
            else -> 1.0
        }
        return (amount * multiplier).toLong()
    }

    private fun parseEta(line: String): String? {
        val value = Regex("(?i)\\bETA\\s+((?:[0-9]+:)?[0-9]{1,2}:[0-9]{2})").find(line)?.groupValues?.get(1) ?: return null
        val parts = value.split(":").mapNotNull { it.toLongOrNull() }
        val seconds = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> return value
        }
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${(seconds / 60) % 60}m remaining"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s remaining"
            else -> "${seconds}s remaining"
        }
    }
}

internal fun formatDownloadBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1000.0 && unit < units.lastIndex) { value /= 1000.0; unit++ }
    return String.format(java.util.Locale.ROOT, if (unit == 0) "%.0f %s" else "%.1f %s", value, units[unit])
}
