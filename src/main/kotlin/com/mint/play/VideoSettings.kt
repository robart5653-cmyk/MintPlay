package com.mint.play

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun VideoSettings() {
    val context = LocalContext.current

    var doubleTapSeek by remember {
        mutableStateOf(
            VideoSettingsStore.getDoubleTapSeekSeconds(context)
        )
    }

    var defaultOrientation by remember {
        mutableStateOf(
            VideoSettingsStore.getDefaultOrientation(context)
        )
    }

    var resumePlayback by remember {
        mutableStateOf(
            VideoSettingsStore.getResumePlayback(context)
        )
    }

    var subtitlesPreference by remember {
        mutableStateOf(
            VideoSettingsStore.getSubtitlesPreference(context)
        )
    }

    var activeDialog by remember {
        mutableStateOf<String?>(null)
    }

    SettingsSection(title = "VIDEO") {

        SettingsRow(
            title = "Double-tap seek",
            subtitle = "Skip forward or backward",
            value = "$doubleTapSeek seconds",
            showArrow = true,
            onClick = {
                activeDialog = "seek"
            }
        )

        SettingsRow(
            title = "Default orientation",
            value = defaultOrientation,
            showArrow = true,
            onClick = {
                activeDialog = "orientation"
            }
        )

        SettingsSwitchRow(
            title = "Resume where you left off",
            subtitle = "Continue videos from the last position",
            checked = resumePlayback,
            onCheckedChange = { enabled ->
                resumePlayback = enabled
                VideoSettingsStore.setResumePlayback(
                    context,
                    enabled
                )
            }
        )

        SettingsRow(
            title = "Subtitles",
            subtitle = "Choose your subtitle preference",
            value = subtitlesPreference,
            showArrow = true,
            showDivider = false,
            onClick = {
                activeDialog = "subtitles"
            }
        )
    }

    when (activeDialog) {
        "seek" -> {
            SettingsChoiceDialog(
                title = "Double-tap seek",
                options = listOf("5", "10", "15", "30"),
                selectedOption = doubleTapSeek.toString(),
                optionLabel = { "$it seconds" },
                onSelect = { selected ->
                    val seconds = selected.toInt()

                    doubleTapSeek = seconds

                    VideoSettingsStore.setDoubleTapSeekSeconds(
                        context,
                        seconds
                    )

                    activeDialog = null
                },
                onDismiss = {
                    activeDialog = null
                }
            )
        }

        "orientation" -> {
            SettingsChoiceDialog(
                title = "Default orientation",
                options = listOf("Auto", "Portrait", "Landscape"),
                selectedOption = defaultOrientation,
                onSelect = { orientation ->
                    defaultOrientation = orientation

                    VideoSettingsStore.setDefaultOrientation(
                        context,
                        orientation
                    )

                    activeDialog = null
                },
                onDismiss = {
                    activeDialog = null
                }
            )
        }

        "subtitles" -> {
            SettingsChoiceDialog(
                title = "Subtitles",
                options = listOf("Auto", "On", "Off"),
                selectedOption = subtitlesPreference,
                onSelect = { preference ->
                    subtitlesPreference = preference

                    VideoSettingsStore.setSubtitlesPreference(
                        context,
                        preference
                    )

                    activeDialog = null
                },
                onDismiss = {
                    activeDialog = null
                }
            )
        }
    }
}

@Composable
private fun SettingsChoiceDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    optionLabel: (String) -> String = { it },
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = {
                                onSelect(option)
                            }
                        )

                        TextButton(
                            onClick = {
                                onSelect(option)
                            }
                        ) {
                            Text(text = optionLabel(option))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}