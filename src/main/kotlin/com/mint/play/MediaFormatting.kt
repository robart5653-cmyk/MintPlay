package com.mint.play

import java.util.Locale

fun formatMediaDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(
            Locale.ROOT,
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    } else {
        String.format(
            Locale.ROOT,
            "%d:%02d",
            minutes,
            seconds
        )
    }
}

fun formatMediaSize(size: Long): String {
    val safeSize = size.coerceAtLeast(0L)

    return when {
        safeSize >= 1024L * 1024 * 1024 ->
            String.format(
                Locale.ROOT,
                "%.1f GB",
                safeSize / (1024.0 * 1024 * 1024)
            )

        safeSize >= 1024L * 1024 ->
            String.format(
                Locale.ROOT,
                "%.1f MB",
                safeSize / (1024.0 * 1024)
            )

        safeSize >= 1024L ->
            String.format(
                Locale.ROOT,
                "%.1f KB",
                safeSize / 1024.0
            )

        else -> "$safeSize B"
    }
}