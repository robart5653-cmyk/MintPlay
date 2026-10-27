package com.mint.play

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MediaPermissions(
    private val activity: ComponentActivity,
    private val onResult: (
        audioGranted: Boolean,
        videoGranted: Boolean
    ) -> Unit
) {
    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            onResult(hasAudioAccess(), hasVideoAccess())

            // All-files access is granted through Android Settings,
            // not through the runtime permission dialog.
            requestAllFilesAccessIfNeeded()
        }

    fun requestIfNeeded() {
       val missing = if (Build.VERSION.SDK_INT >= 33) {
            buildList {
                if (!hasAudioAccess()) {
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                }
                if (!hasVideoAccess()) {
                    add(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }
        } else {
            if (!hasAudioAccess() || !hasVideoAccess()) {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                emptyList()
            }
        }

       if (missing.isNotEmpty()) {
           launcher.launch(missing.toTypedArray())
       } else {
           onResult(hasAudioAccess(), hasVideoAccess())
            requestAllFilesAccessIfNeeded()
        }
    }

    fun hasAudioAccess(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasVideoAccess(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllFilesAccess(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()
    }

    private fun requestAllFilesAccessIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        }
    }
}