package com.mint.play

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(onDownloadsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onDownloadsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val iconColor = Accent
                val strokeWidth = 1.8.dp.toPx()
                val centerX = size.width / 2f

                drawCircle(color = iconColor, style = Stroke(width = strokeWidth))
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
    }
}
