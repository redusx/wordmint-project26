package com.example.wordstore.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.WordMintTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * The only way glass is drawn in WordMint. Use it for things that float above moving content, and
 * never behind text a person reads for meaning. See DESIGN_RULES.md.
 *
 * API 31+ gets real backdrop blur. Below that the tint rises until no blur is needed to read it.
 */
@Composable
fun GlassSurface(
    hazeState: HazeState,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = WordMintTheme.colors
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        modifier = modifier
            .shadow(elevation = 18.dp, shape = shape, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
            .clip(shape)
            .then(
                if (supportsBlur) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = colors.canvas,
                            tints = listOf(HazeTint(colors.glassTint)),
                            blurRadius = 22.dp,
                            noiseFactor = 0f
                        )
                    )
                } else {
                    Modifier.background(colors.glassTintOpaque)
                }
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0f to colors.glassRimTop,
                        0.55f to Color.Transparent,
                        1f to colors.glassRimBottom
                    )
                ),
                shape
            ),
        content = content
    )
}
