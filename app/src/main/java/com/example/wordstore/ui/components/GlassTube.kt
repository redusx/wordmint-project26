package com.example.wordstore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.Motion
import com.example.wordstore.ui.theme.WordMintTheme

enum class TubeOrientation { VERTICAL, HORIZONTAL }

/**
 * The signature element: a glass vessel whose liquid level always shows a real value.
 *
 * It moves only when [level] changes. It never idles, so it cannot become decoration.
 *
 * @param level from 0 to 1
 * @param graduations number of tick marks beside a vertical tube; 0 for none
 */
@Composable
fun GlassTube(
    level: Float,
    modifier: Modifier = Modifier,
    orientation: TubeOrientation = TubeOrientation.VERTICAL,
    graduations: Int = 0,
    liquid: Color = WordMintTheme.colors.liquid,
    liquidDeep: Color = WordMintTheme.colors.liquidDeep
) {
    val colors = WordMintTheme.colors
    val target = level.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, Motion.liquid(), label = "tube_level")

    Canvas(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
        }
    ) {
        val tickSpace = if (graduations > 0) 18.dp.toPx() else 0f
        val body = Rect(0f, 0f, size.width - tickSpace, size.height)
        val horizontal = orientation == TubeOrientation.HORIZONTAL
        val radius = (if (horizontal) body.height else body.width) / 2f
        val shape = RoundRect(body, CornerRadius(radius))
        val vessel = Path().apply { addRoundRect(shape) }

        drawPath(vessel, colors.tubeGlass)

        clipPath(vessel) {
            if (horizontal) {
                drawLiquidHorizontal(body, animated, liquid, liquidDeep)
            } else {
                drawLiquidVertical(body, animated, liquid, liquidDeep, colors.tubeHighlight)
            }
        }

        // Light falls from the top left, along the inside of the wall.
        val highlightInset = radius * 0.42f
        if (horizontal) {
            drawLine(
                color = colors.tubeHighlight,
                start = Offset(radius, highlightInset),
                end = Offset((body.right - radius).coerceAtLeast(radius), highlightInset),
                strokeWidth = (body.height * 0.1f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        } else {
            drawLine(
                color = colors.tubeHighlight,
                start = Offset(highlightInset, radius),
                end = Offset(highlightInset, body.bottom - radius),
                strokeWidth = (body.width * 0.09f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        }

        val rimWidth = 1.dp.toPx()
        drawPath(vessel, colors.tubeRim, style = Stroke(rimWidth))

        if (graduations > 0) {
            val usable = body.height - radius * 2
            for (tick in 0..graduations) {
                val y = body.bottom - radius - usable * tick / graduations
                val major = tick % 5 == 0 || graduations <= 5
                val length = if (major) 12.dp.toPx() else 7.dp.toPx()
                drawLine(
                    color = colors.inkMuted.copy(alpha = if (major) 0.8f else 0.45f),
                    start = Offset(body.right + 6.dp.toPx(), y),
                    end = Offset(body.right + 6.dp.toPx() + length, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

private fun DrawScope.drawLiquidVertical(body: Rect, level: Float, liquid: Color, deep: Color, highlight: Color) {
    if (level <= 0f) return
    val radius = body.width / 2f
    val usable = body.height - radius
    val surfaceY = body.bottom - usable * level
    val meniscus = (body.width * 0.1f) * (if (level >= 0.999f) 0f else 1f)

    val path = Path().apply {
        moveTo(body.left, surfaceY - meniscus)
        quadraticBezierTo(body.center.x, surfaceY + meniscus, body.right, surfaceY - meniscus)
        lineTo(body.right, body.bottom)
        lineTo(body.left, body.bottom)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(liquid, deep), startY = surfaceY, endY = body.bottom))

    // The surface catches light, which is what makes it read as liquid.
    val surface = Path().apply {
        moveTo(body.left, surfaceY - meniscus)
        quadraticBezierTo(body.center.x, surfaceY + meniscus, body.right, surfaceY - meniscus)
    }
    drawPath(surface, highlight, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawLiquidHorizontal(body: Rect, level: Float, liquid: Color, deep: Color) {
    if (level <= 0f) return
    drawRect(
        brush = Brush.horizontalGradient(listOf(deep, liquid), startX = 0f, endX = body.width * level),
        topLeft = Offset(0f, 0f),
        size = Size(body.width * level, body.height)
    )
}
