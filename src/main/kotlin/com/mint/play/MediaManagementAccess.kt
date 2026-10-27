package com.mint.play

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun MediaManagementAccess() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccess by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                MediaStore.canManageMedia(context)
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    MediaStore.canManageMedia(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Media management access",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = if (hasAccess) {
                "Access is enabled. Android may still ask for confirmation for some media changes."
            } else {
                "Allow special access to try reducing repeated confirmations when renaming or deleting media."
            },
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_MANAGE_MEDIA,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            },
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                if (hasAccess) "Check access settings"
                else "Open media access settings"
            )
        }
    }
}