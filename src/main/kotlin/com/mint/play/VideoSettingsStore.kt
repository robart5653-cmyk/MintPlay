package com.mint.play

import android.content.Context
import android.content.SharedPreferences

object VideoSettingsStore {

    private const val PREFS_NAME = "mintplay_video_settings"

    private const val KEY_DOUBLE_TAP_SEEK = "double_tap_seek_seconds"
    private const val KEY_DEFAULT_ORIENTATION = "default_orientation"
    private const val KEY_RESUME_PLAYBACK = "resume_playback"

    private fun preferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    fun getDoubleTapSeekSeconds(context: Context): Int {
        return preferences(context).getInt(KEY_DOUBLE_TAP_SEEK, 10)
    }

    fun setDoubleTapSeekSeconds(context: Context, seconds: Int) {
        val validValues = setOf(5, 10, 15, 30)

        if (seconds in validValues) {
            preferences(context).edit()
                .putInt(KEY_DOUBLE_TAP_SEEK, seconds)
                .apply()
        }
    }

    fun getDefaultOrientation(context: Context): String {
        return preferences(context).getString(
            KEY_DEFAULT_ORIENTATION,
            "Auto"
        ) ?: "Auto"
    }

    fun setDefaultOrientation(context: Context, orientation: String) {
        if (orientation in setOf("Auto", "Portrait", "Landscape")) {
            preferences(context).edit()
                .putString(KEY_DEFAULT_ORIENTATION, orientation)
                .apply()
        }
    }

    fun getResumePlayback(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_RESUME_PLAYBACK, true)
    }

    fun setResumePlayback(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_RESUME_PLAYBACK, enabled)
            .apply()
    }

    private const val KEY_PLAYBACK_POSITION_PREFIX = "playback_position:"

    fun getPlaybackPosition(context: Context, uri: String): Long {
        return preferences(context).getLong(KEY_PLAYBACK_POSITION_PREFIX + uri, 0L)
    }

    fun setPlaybackPosition(context: Context, uri: String, positionMs: Long) {
        val editor = preferences(context).edit()
        val key = KEY_PLAYBACK_POSITION_PREFIX + uri

        if (positionMs > 0L) {
            editor.putLong(key, positionMs)
        } else {
            editor.remove(key)
        }

        editor.apply()
    }

    fun clearPlaybackPositions(context: Context) {
        val prefs = preferences(context)
        val editor = prefs.edit()

        prefs.all.keys
            .filter { it.startsWith(KEY_PLAYBACK_POSITION_PREFIX) }
            .forEach { editor.remove(it) }

        editor.apply()
    }
}
