package com.mint.play

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TOP_BAR_PREFS = "mintplay_top_bar"
private const val YOUTUBE_BUTTON_CLICKED_KEY = "youtube_button_clicked"

@Composable
fun TopBar() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(TOP_BAR_PREFS, Context.MODE_PRIVATE)
    }
    var isClicked by remember {
        mutableStateOf(preferences.getBoolean(YOUTUBE_BUTTON_CLICKED_KEY, false))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                isClicked = !isClicked
                preferences.edit()
                    .putBoolean(YOUTUBE_BUTTON_CLICKED_KEY, isClicked)
                    .apply()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val iconColor = if (isClicked) Color(0xFF4DD4AC) else PrimaryText
                val strokeWidth = 1.8.dp.toPx()
                val centerX = size.width / 2f

                // Circular outline
                drawCircle(
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )

                // Download arrow
                drawLine(
                    color = iconColor,
                    start = Offset(centerX, size.height * 0.23f),
                    end = Offset(centerX, size.height * 0.62f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.32f, size.height * 0.48f),
                    end = Offset(centerX, size.height * 0.64f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.68f, size.height * 0.48f),
                    end = Offset(centerX, size.height * 0.64f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.29f, size.height * 0.76f),
                    end = Offset(size.width * 0.71f, size.height * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Mint",
            color = PrimaryText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
        Text(
            text = "Play",
            color = Color(0xFF4DD4AC),
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
    }
}
