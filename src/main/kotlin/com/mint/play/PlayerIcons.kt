package com.mint.play

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun HeadphoneIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()

        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(
                size.width * 0.16f,
                size.height * 0.08f
            ),
            size = Size(
                size.width * 0.68f,
                size.height * 0.72f
            ),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(
                size.width * 0.10f,
                size.height * 0.45f
            ),
            size = Size(
                size.width * 0.23f,
                size.height * 0.36f
            ),
            cornerRadius = CornerRadius(size.width * 0.07f),
            style = Stroke(width = strokeWidth)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(
                size.width * 0.67f,
                size.height * 0.45f
            ),
            size = Size(
                size.width * 0.23f,
                size.height * 0.36f
            ),
            cornerRadius = CornerRadius(size.width * 0.07f),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
internal fun OrientationLockIcon(
    modifier: Modifier = Modifier,
    locked: Boolean,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(
                size.width * 0.19f,
                size.height * 0.43f
            ),
            size = Size(
                size.width * 0.62f,
                size.height * 0.47f
            ),
            cornerRadius = CornerRadius(size.width * 0.09f),
            style = Stroke(width = strokeWidth)
        )

        val shackle = Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.46f)
            lineTo(size.width * 0.32f, size.height * 0.31f)

            quadraticTo(
                size.width * 0.32f,
                size.height * 0.10f,
                size.width * 0.50f,
                size.height * 0.10f
            )

            quadraticTo(
                size.width * 0.68f,
                size.height * 0.10f,
                size.width * 0.68f,
                if (locked) size.height * 0.31f else size.height * 0.23f
            )

            if (locked) {
                lineTo(size.width * 0.68f, size.height * 0.46f)
            }
        }

        drawPath(
            path = shackle,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawCircle(
            color = color,
            radius = size.width * 0.045f,
            center = Offset(
                size.width * 0.50f,
                size.height * 0.61f
            )
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.50f,
                size.height * 0.65f
            ),
            end = Offset(
                size.width * 0.50f,
                size.height * 0.74f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun HollowPlayIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.10f)
            lineTo(size.width * 0.82f, size.height * 0.50f)
            lineTo(size.width * 0.18f, size.height * 0.90f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}