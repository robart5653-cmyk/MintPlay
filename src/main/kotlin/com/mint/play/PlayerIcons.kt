package com.mint.play

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
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

@Composable
internal fun RotationModeIcon(
    modifier: Modifier = Modifier,
    isLandscape: Boolean,
    color: Color
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val screenWidth = size.width * (if (isLandscape) 0.54f else 0.34f)
        val screenHeight = size.height * (if (isLandscape) 0.34f else 0.54f)
        val screenLeft = (size.width - screenWidth) / 2f
        val screenTop = (size.height - screenHeight) / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(size.width * 0.035f),
            style = Stroke(width = stroke)
        )
    }
}


@Composable
internal fun AudioQueueIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(size.width * 0.10f, size.height * 0.25f), Offset(size.width * 0.53f, size.height * 0.25f), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.10f, size.height * 0.48f), Offset(size.width * 0.53f, size.height * 0.48f), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.10f, size.height * 0.71f), Offset(size.width * 0.43f, size.height * 0.71f), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.77f, size.height * 0.20f), Offset(size.width * 0.77f, size.height * 0.65f), stroke, cap = StrokeCap.Round)
        drawCircle(color, size.width * 0.085f, Offset(size.width * 0.65f, size.height * 0.72f))
    }
}

@Composable
internal fun AudioShuffleIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val upperPath = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.25f)
            lineTo(size.width * 0.28f, size.height * 0.25f)
            quadraticTo(size.width * 0.40f, size.height * 0.25f, size.width * 0.50f, size.height * 0.43f)
            lineTo(size.width * 0.69f, size.height * 0.68f)
            lineTo(size.width * 0.88f, size.height * 0.68f)
        }
        val lowerPath = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.75f)
            lineTo(size.width * 0.28f, size.height * 0.75f)
            quadraticTo(size.width * 0.40f, size.height * 0.75f, size.width * 0.50f, size.height * 0.57f)
            lineTo(size.width * 0.69f, size.height * 0.32f)
            lineTo(size.width * 0.88f, size.height * 0.32f)
        }
        val arrows = Path().apply {
            moveTo(size.width * 0.73f, size.height * 0.52f)
            lineTo(size.width * 0.88f, size.height * 0.68f)
            lineTo(size.width * 0.73f, size.height * 0.84f)
            moveTo(size.width * 0.73f, size.height * 0.16f)
            lineTo(size.width * 0.88f, size.height * 0.32f)
            lineTo(size.width * 0.73f, size.height * 0.48f)
        }
        val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(upperPath, color, style = style)
        drawPath(lowerPath, color, style = style)
        drawPath(arrows, color, style = style)
    }
}

@Composable
internal fun AudioRepeatIcon(
    modifier: Modifier = Modifier,
    color: Color,
    repeatOne: Boolean = false
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.36f)
            lineTo(size.width * 0.78f, size.height * 0.25f)
            quadraticTo(size.width * 0.78f, size.height * 0.15f, size.width * 0.66f, size.height * 0.15f)
            lineTo(size.width * 0.28f, size.height * 0.15f)
            lineTo(size.width * 0.38f, size.height * 0.05f)
            moveTo(size.width * 0.28f, size.height * 0.15f)
            lineTo(size.width * 0.38f, size.height * 0.25f)
            moveTo(size.width * 0.22f, size.height * 0.64f)
            lineTo(size.width * 0.22f, size.height * 0.75f)
            quadraticTo(size.width * 0.22f, size.height * 0.85f, size.width * 0.34f, size.height * 0.85f)
            lineTo(size.width * 0.72f, size.height * 0.85f)
            lineTo(size.width * 0.62f, size.height * 0.75f)
            moveTo(size.width * 0.72f, size.height * 0.85f)
            lineTo(size.width * 0.62f, size.height * 0.95f)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (repeatOne) {
            val one = Path().apply {
                moveTo(size.width * 0.48f, size.height * 0.43f)
                lineTo(size.width * 0.56f, size.height * 0.37f)
                lineTo(size.width * 0.56f, size.height * 0.62f)
            }
            drawPath(one, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(color, Offset(size.width * 0.49f, size.height * 0.62f), Offset(size.width * 0.63f, size.height * 0.62f), stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun PreviousTrackIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(size.width * 0.20f, size.height * 0.18f), Offset(size.width * 0.20f, size.height * 0.82f), stroke, cap = StrokeCap.Round)
        val triangle = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.18f)
            lineTo(size.width * 0.34f, size.height * 0.50f)
            lineTo(size.width * 0.78f, size.height * 0.82f)
            close()
        }
        drawPath(triangle, color)
    }
}

@Composable
internal fun NextTrackIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(size.width * 0.80f, size.height * 0.18f), Offset(size.width * 0.80f, size.height * 0.82f), stroke, cap = StrokeCap.Round)
        val triangle = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.18f)
            lineTo(size.width * 0.66f, size.height * 0.50f)
            lineTo(size.width * 0.22f, size.height * 0.82f)
            close()
        }
        drawPath(triangle, color)
    }
}
