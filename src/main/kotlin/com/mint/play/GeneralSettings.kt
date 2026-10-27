package com.mint.play

import android.R.color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextFieldDefaults

private val supportedLanguages = listOf(
    "Afrikaans", "አማርኛ", "العربية", "Azərbaycan", "বাংলা",
    "Беларуская", "Български", "မြန်မာ", "Català", "简体中文",
    "繁體中文", "Hrvatski", "Čeština", "Dansk", "Nederlands",
    "English (US)", "Eesti", "فارسی", "Suomi", "Français",
    "Deutsch", "Ελληνικά", "ગુજરાતી", "עברית", "हिन्दी",
    "Magyar", "Íslenska", "Bahasa Indonesia", "Italiano", "日本語",
    "ಕನ್ನಡ", "Қазақша", "ខ្មែរ", "한국어", "ລາວ",
    "Latviešu", "Lietuvių", "Македонски", "മലയാളം", "मराठी",
    "नेपाली", "Norsk", "ଓଡ଼ିଆ", "پښتو", "Polski",
    "Português", "ਪੰਜਾਬੀ", "Română", "Русский", "Српски",
    "සිංහල", "Slovenčina", "Slovenščina", "Español", "Kiswahili",
    "Svenska", "தமிழ்", "తెలుగు", "ไทย", "Тоҷикӣ",
    "Türkçe", "Українська", "اردو", "Oʻzbekcha", "Tiếng Việt",
    "isiZulu"
)

private const val GENERAL_PREFS = "mintplay_general_settings"
private const val LANGUAGE_KEY = "selected_language"
private const val HISTORY_KEY = "show_history"

@Composable
fun GeneralSettings() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            GENERAL_PREFS,
            android.content.Context.MODE_PRIVATE
        )
    }

    var selectedLanguage by remember {
        mutableStateOf(
            preferences.getString(LANGUAGE_KEY, "English (US)")
                ?: "English (US)"
        )
    }

    var showHistory by remember {
        mutableStateOf(preferences.getBoolean(HISTORY_KEY, false))
    }

    var showLanguageDialog by remember {
        mutableStateOf(false)
    }

    var languageSearch by remember {
        mutableStateOf("")
    }

    SettingsSection(title = "GENERAL") {
        SettingsRow(
            title = "Language",
            subtitle = "App display language",
            value = selectedLanguage,
            showArrow = true,
            showDivider = true,
            onClick = {
                languageSearch = ""
                showLanguageDialog = true
            }
        )

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

    if (showLanguageDialog) {
        val filteredLanguages = supportedLanguages.filter { language ->
            language.contains(languageSearch, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                showLanguageDialog = false
            },
            containerColor = NavBackground,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Choose language",
                    color = PrimaryText
                )
            },
            text = {
                Column {
                   OutlinedTextField(
    value = languageSearch,
    onValueChange = { languageSearch = it },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    label = {
        Text(
            text = "Search languages",
            color = PrimaryText
        )
    },
    textStyle = TextStyle(color = PrimaryText),
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text
    ),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF4DD4AC),
        unfocusedBorderColor = Color(0xFF858A96),
        cursorColor = Color(0xFF4DD4AC),
        focusedLabelColor = Color(0xFF4DD4AC),
        unfocusedLabelColor = Color(0xFF858A96)
    )
)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        items(
                            items = filteredLanguages,
                            key = { it }
                        ) { language ->
                            TextButton(
                                onClick = {
                                    selectedLanguage = language

                                    preferences.edit()
                                        .putString(LANGUAGE_KEY, language)
                                        .apply()

                                    showLanguageDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = language,
                                    color = if (language == selectedLanguage) {
                                        Accent
                                    } else {
                                        PrimaryText
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false }
                ) {
                    Text(
                        text = "Close",
                        color = PrimaryText
                    )
                }
            }
        )
    }
}