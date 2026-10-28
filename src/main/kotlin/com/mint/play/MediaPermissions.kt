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

class MediaPermissions(
    private val activity: ComponentActivity,
    private val onResult: (audioGranted: Boolean, videoGranted: Boolean) -> Unit
) {
    private val legacyStorageLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            val granted = hasAudioAccess()
            onResult(granted, granted)
        }

    fun requestIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestAllFilesAccessIfNeeded()
            val granted = hasAllFilesAccess()
            if (granted) onResult(true, true)
            return
        }

        if (hasLegacyStorageAccess()) {
            onResult(true, true)
        } else {
            legacyStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun hasAudioAccess(): Boolean = hasStorageAccess()

    fun hasVideoAccess(): Boolean = hasStorageAccess()

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()

    private fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasLegacyStorageAccess()
        }
    }

    private fun hasLegacyStorageAccess(): Boolean {
        return activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
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
