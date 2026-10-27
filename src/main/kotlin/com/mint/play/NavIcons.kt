
package com.mint.play

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Duotone style: bold outline + soft translucent fill. Tint-friendly (white base).
private val Ink = SolidColor(Color.White)
private val Wash = SolidColor(Color.White.copy(alpha = 0.22f))

private fun createIcon(
    name: String,
    draw: ImageVector.Builder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply(draw).build()

/** Soft-filled shape with a bold rounded outline. */
private fun ImageVector.Builder.soft(
    width: Float = 1.8f,
    evenOdd: Boolean = false,
        block: PathBuilder.() -> Unit
) = path(
    fill = Wash,
    stroke = Ink,
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
    pathBuilder = block
)

/** Solid white shape, rounded by a stroke of the same color. */
private fun ImageVector.Builder.solid(
    width: Float = 1.8f,
    block: PathBuilder.() -> Unit
) = path(
    fill = Ink,
    stroke = Ink,
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = block
)

/** Outline only, no fill. */
private fun ImageVector.Builder.line(
    width: Float = 1.8f,
    block: PathBuilder.() -> Unit
) = path(
    fill = null,
    stroke = Ink,
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = block
)

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
    arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
    close()
}

private fun rad(deg: Float) = (deg * PI / 180.0).toFloat()

/** Clapperboard with a play button. */
internal val VideoIcon = createIcon("Video") {
    // Board body
    soft {
        moveTo(3.5f, 10f)
        horizontalLineTo(20.5f)
        verticalLineTo(17.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 18f, 20f)
        horizontalLineTo(6f)
        arcTo(2.5f, 2.5f, 0f, false, true, 3.5f, 17.5f)
        close()
    }
    // Clapper bar
    soft {
        moveTo(3.5f, 8.2f)
        verticalLineTo(7.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 6f, 5f)
        horizontalLineTo(18f)
        arcTo(2.5f, 2.5f, 0f, false, true, 20.5f, 7.5f)
        verticalLineTo(8.2f)
        close()
    }
    // Clapper stripes
    line {
        moveTo(8.6f, 8.2f); lineTo(11f, 5.2f)
        moveTo(13.6f, 8.2f); lineTo(16f, 5.2f)
    }
    // Play triangle
    solid(width = 1.6f) {
        moveTo(10.4f, 12.9f)
        lineTo(14.6f, 15f)
        lineTo(10.4f, 17.1f)
        close()
    }
}

/** Beamed double note with a sparkle. */
internal val MusicIcon = createIcon("Music") {
    // Beam
    solid(width = 1.4f) {
        moveTo(9.6f, 6.6f)
        lineTo(19.1f, 4.4f)
        lineTo(19.1f, 8f)
        lineTo(9.6f, 10.2f)
        close()
    }
    // Stems
    line {
        moveTo(9.6f, 17.5f); verticalLineTo(7f)
        moveTo(19.1f, 15.5f); verticalLineTo(5f)
    }
    // Note heads
    soft { circle(cx = 7f, cy = 17.5f, r = 2.7f) }
    soft { circle(cx = 16.5f, cy = 15.5f, r = 2.7f) }
    // Sparkle
    solid(width = 0.6f) {
        moveTo(4.6f, 2.8f)
        quadTo(4.9f, 4.7f, 6.8f, 5f)
        quadTo(4.9f, 5.3f, 4.6f, 7.2f)
        quadTo(4.3f, 5.3f, 2.4f, 5f)
        quadTo(4.3f, 4.7f, 4.6f, 2.8f)
        close()
    }
}

/** Open-end wrench set at an angle, with a hanging hole. */
internal val ToolsIcon = createIcon("Tools") {
    group(rotate = 45f, pivotX = 12f, pivotY = 12f) {
        soft {
            // Jaw opening at the top, round head, tapered handle
            moveTo(10.4f, 2.9f)
            verticalLineTo(6.6f)
            horizontalLineTo(13.6f)
            verticalLineTo(2.9f)
            arcTo(4.4f, 4.4f, 0f, false, true, 13.7f, 11.2f)
            verticalLineTo(19f)
            arcTo(1.7f, 1.7f, 0f, false, true, 10.3f, 19f)
            verticalLineTo(11.2f)
            arcTo(4.4f, 4.4f, 0f, false, true, 10.4f, 2.9f)
            close()
        }
        // Hanging hole
        line(width = 1.5f) { circle(cx = 12f, cy = 17.2f, r = 0.5f) }
    }
}

/** Chunky six-tooth gear with a center hole. */
internal val SettingsIcon = createIcon("Settings") {
    soft(evenOdd = true) {
        val cx = 12f
        val cy = 12f
        val inner = 6.9f
        val outer = 9.4f
        val teeth = 6
        val step = 360f / teeth
        val innerHalf = 17f
        val outerHalf = 10f

        fun px(r: Float, deg: Float) = cx + r * cos(rad(deg))
        fun py(r: Float, deg: Float) = cy + r * sin(rad(deg))

        val start = -90f - innerHalf
        moveTo(px(inner, start), py(inner, start))
        for (i in 0 until teeth) {
            val c = -90f + i * step
            lineTo(px(outer, c - outerHalf), py(outer, c - outerHalf))
            lineTo(px(outer, c + outerHalf), py(outer, c + outerHalf))
            lineTo(px(inner, c + innerHalf), py(inner, c + innerHalf))
            val next = c + step - innerHalf
            arcTo(inner, inner, 0f, false, true, px(inner, next), py(inner, next))
        }
        close()
        // Center hole (cut out thanks to EvenOdd)
        circle(cx = cx, cy = cy, r = 2.8f)
    }
}
