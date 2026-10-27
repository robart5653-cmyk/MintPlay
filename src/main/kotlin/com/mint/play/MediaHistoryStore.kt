package com.mint.play

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

object MediaHistoryStore {
    const val PREFS_NAME = "mintplay_general_settings"
    const val SHOW_HISTORY_KEY = "show_history"
    const val VIDEO_HISTORY_KEY = "recent_video_history"

    private const val MAX_HISTORY_ITEMS = 20

    private fun preferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    fun isHistoryEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(SHOW_HISTORY_KEY, false)
    }

    fun getRecentVideoUris(context: Context): List<String> {
        return preferences(context)
            .getString(VIDEO_HISTORY_KEY, "")
            .orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_HISTORY_ITEMS)
    }

    fun addVideo(context: Context, uri: Uri) {
        val uriString = uri.toString()
        val updatedHistory = (listOf(uriString) +
            getRecentVideoUris(context).filterNot { it == uriString })
            .take(MAX_HISTORY_ITEMS)

        preferences(context).edit()
            .putString(VIDEO_HISTORY_KEY, updatedHistory.joinToString("\n"))
            .apply()
    }
}
