package com.mint.play

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val GENERAL_PREFS = "mintplay_general_settings"
private const val HISTORY_KEY = "show_history"

@Composable
fun GeneralSettings() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            GENERAL_PREFS,
            Context.MODE_PRIVATE
        )
    }

    var showHistory by remember {
        mutableStateOf(preferences.getBoolean(HISTORY_KEY, false))
    }

    SettingsSection(title = "GENERAL") {
        SettingsSwitchRow(
            title = "Show history",
            subtitle = "Show recently played media",
            checked = showHistory,
            onCheckedChange = { enabled ->
                showHistory = enabled
                preferences.edit()
                    .putBoolean(HISTORY_KEY, enabled)
                    .apply()
            },
            showDivider = false
        )
    }
}
