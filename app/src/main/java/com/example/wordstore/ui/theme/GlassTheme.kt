package com.example.wordstore.ui.theme

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * VisionOS / Spatial Design - Liquid Glass Theme System
 * 
 * Design Elements:
 * - Frosted Glass surfaces with subtle transparency
 * - Specular rim lighting (top-lit borders)
 * - Volumetric shadows with color glow
 * - Antigravity levitation animations
 */

// ====================
// Glass Color Palette - Pure Gray Theme (NO COLORS)
// ====================
object GlassColors {
    // Primary Accent (light gray - no color)
    val Accent = Color(0xFFE0E0E0)  // Light gray
    val AccentDim = Color(0xFFBDBDBD) // Medium gray
    
    // Dark Gray Base Colors
    val DarkGray900 = Color(0xFF121212)  // Darkest
    val DarkGray850 = Color(0xFF1A1A1A)
    val DarkGray800 = Color(0xFF1E1E1E)
    val DarkGray700 = Color(0xFF2D2D2D)
    val DarkGray600 = Color(0xFF3D3D3D)
    val DarkGray500 = Color(0xFF5C5C5C)
    val DarkGray400 = Color(0xFF757575)
    val DarkGray300 = Color(0xFF9E9E9E)
    
    // Glass Surface Colors (Dark Theme - Primary)
    val GlassSurfaceDark = DarkGray900
    val GlassCardDark = Color.White.copy(alpha = 0.05f)
    val GlassHighlightDark = Color.White.copy(alpha = 0.08f)
    val GlassBorderTopDark = Color.White.copy(alpha = 0.12f)
    val GlassBorderBottomDark = Color.Transparent
    
    // Glass Surface Colors (Light Theme)
    val GlassSurfaceLight = Color(0xFFF5F5F5)
    val GlassCardLight = Color.White.copy(alpha = 0.8f)
    val GlassHighlightLight = Color.White.copy(alpha = 0.95f)
    val GlassBorderTopLight = Color.White.copy(alpha = 0.9f)
    val GlassBorderBottomLight = Color.White.copy(alpha = 0.3f)
    
    // Solid Background (NO GRADIENT)
    val DarkBackground = DarkGray900
    val DarkCardBackground = DarkGray800
    val DarkDialogBackground = DarkGray850
    
    val LightBackground = Color(0xFFF5F5F5)
    val LightCardBackground = Color.White
    
    // Text Colors
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.7f)
    val TextTertiary = Color.White.copy(alpha = 0.5f)
    
    // Glow/Shadow (very subtle, gray)
    val GlowAccent = Color.White.copy(alpha = 0.08f)
    val AmbientGlow = Color.White.copy(alpha = 0.04f)
    
    // Status Colors (muted)
    val Error = Color(0xFFCF6679)  // Muted red
    val Success = Color(0xFF81C784) // Muted green
}

// ====================
// Glass Modifiers
// ====================
object GlassModifiers {
    
    /**
     * Apply frosted glass background
     */
    @Composable
    fun Modifier.glassBackground(
        isDarkTheme: Boolean = true,
        alpha: Float = 1f
    ): Modifier {
        val glassSurface = if (isDarkTheme) GlassColors.GlassCardDark else GlassColors.GlassCardLight
        val glassHighlight = if (isDarkTheme) GlassColors.GlassHighlightDark else GlassColors.GlassHighlightLight
        
        return this.background(
            Brush.linearGradient(
                colors = listOf(
                    glassHighlight.copy(alpha = glassHighlight.alpha * alpha),
                    glassSurface.copy(alpha = glassSurface.alpha * alpha),
                    glassSurface.copy(alpha = glassSurface.alpha * 0.5f * alpha)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
    }
    
    /**
     * Apply specular rim light border (top-lit glass effect)
     */
    @Composable
    fun Modifier.specularBorder(
        isDarkTheme: Boolean = true,
        shape: Shape = RoundedCornerShape(24.dp),
        width: Dp = 1.dp
    ): Modifier {
        val borderTop = if (isDarkTheme) GlassColors.GlassBorderTopDark else GlassColors.GlassBorderTopLight
        val borderBottom = if (isDarkTheme) GlassColors.GlassBorderBottomDark else GlassColors.GlassBorderBottomLight
        
        return this.border(
            width = width,
            brush = Brush.verticalGradient(
                colors = listOf(borderTop, borderBottom)
            ),
            shape = shape
        )
    }
    
    /**
     * Apply volumetric glow shadow
     */
    @Composable
    fun Modifier.volumetricGlow(
        glowColor: Color = GlassColors.Accent,
        glowAlpha: Float = 0.3f,
        offsetY: Dp = 8.dp,
        blurRadius: Dp = 20.dp,
        shape: Shape = RoundedCornerShape(24.dp)
    ): Modifier {
        return this.drawBehind {
            // Glow effect drawn behind
        }
    }
    
    /**
     * Apply blur effect (Android 12+)
     */
    @Composable
    fun Modifier.glassBlur(radius: Dp = 16.dp): Modifier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.blur(radius = radius)
        } else {
            this // No blur on older devices
        }
    }
}

// ====================
// Levitation Animation Provider
// ====================
@Composable
fun rememberLevitationState(): LevitationState {
    val infiniteTransition = rememberInfiniteTransition(label = "levitation")
    
    val translationY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    
    val rotationZ by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotateZ"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    return remember(translationY, rotationZ, glowAlpha) {
        LevitationState(translationY, rotationZ, glowAlpha)
    }
}

data class LevitationState(
    val translationY: Float,
    val rotationZ: Float,
    val glowAlpha: Float
)

// ====================
// Background (Solid Color - No Gradient)
// ====================
@Composable
fun GlassBackground(
    isDarkTheme: Boolean = true
): Color {
    return if (isDarkTheme) {
        GlassColors.DarkBackground
    } else {
        GlassColors.LightBackground
    }
}

