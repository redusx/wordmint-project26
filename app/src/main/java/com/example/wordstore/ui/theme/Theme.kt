package com.example.wordstore.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme with complete Material 3 token coverage.
 * Premium, sophisticated palette for WordStore.
 */
private val LightColorScheme = lightColorScheme(
    // Primary
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    
    // Secondary
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    
    // Tertiary
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    
    // Error
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    
    // Background & Surface
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceTint = md_theme_light_surfaceTint,
    
    // Outline
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    
    // Inverse
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    
    // Scrim
    scrim = md_theme_light_scrim,
    
    // Surface containers
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest
)

/**
 * Dark color scheme with complete Material 3 token coverage.
 * Premium, sophisticated palette for WordStore.
 */
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    
    // Secondary
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    
    // Tertiary
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    
    // Error
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    
    // Background & Surface
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceTint = md_theme_dark_surfaceTint,
    
    // Outline
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    
    // Inverse
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    
    // Scrim
    scrim = md_theme_dark_scrim,
    
    // Surface containers
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest
)

/**
 * Material 3 shape definitions for consistent corner radii.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(CornerRadius.ExtraSmall),
    small = RoundedCornerShape(CornerRadius.Small),
    medium = RoundedCornerShape(CornerRadius.Medium),
    large = RoundedCornerShape(CornerRadius.Large),
    extraLarge = RoundedCornerShape(CornerRadius.ExtraLarge)
)

/**
 * Animation specifications for consistent motion throughout the app.
 * Following Material Design motion principles.
 */
object AnimationSpecs {
    /** Ultra-short duration for micro-interactions */
    const val QuickDuration = 100
    
    /** Short duration for micro-interactions */
    const val ShortDuration = 150
    
    /** Standard duration for most transitions */
    const val StandardDuration = 300
    
    /** Medium duration for complex transitions */
    const val MediumDuration = 400
    
    /** Long duration for complex transitions */
    const val LongDuration = 500
    
    /** Extra long duration for elaborate animations */
    const val ExtraLongDuration = 700
    
    /** Tween animation for fade effects */
    fun <T> fadeTween() = tween<T>(
        durationMillis = StandardDuration,
        easing = FastOutSlowInEasing
    )
    
    /** Quick tween for snappy responses */
    fun <T> quickTween() = tween<T>(
        durationMillis = ShortDuration,
        easing = FastOutSlowInEasing
    )
    
    /** Spring animation for bouncy effects */
    fun <T> springBounce() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /** Spring animation for snappy responses */
    fun <T> springSnappy() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    /** Spring animation for gentle movements */
    fun <T> springGentle() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Gradient utilities for premium visual effects.
 */
object Gradients {
    /** Primary gradient for buttons and highlights */
    val Primary = Brush.horizontalGradient(
        colors = listOf(
            BrandColors.GradientStart,
            BrandColors.GradientMiddle,
            BrandColors.GradientEnd
        )
    )
    
    /** Vertical primary gradient */
    val PrimaryVertical = Brush.verticalGradient(
        colors = listOf(
            BrandColors.GradientStart,
            BrandColors.GradientEnd
        )
    )
    
    /** Subtle card gradient for light theme */
    val CardLight = Brush.verticalGradient(
        colors = listOf(
            BrandColors.CardGradientStart,
            BrandColors.CardGradientEnd
        )
    )
    
    /** Glassmorphism overlay effect */
    val GlassOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.05f)
        )
    )
}

/**
 * WordStore app theme with Material 3, dynamic color support, and proper theming.
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic color on Android 12+ (defaults to false for brand consistency)
 * @param content The composable content to apply the theme to
 */
@Composable
fun WordStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent brand identity
    content: @Composable () -> Unit
) {
    // Determine color scheme based on dynamic color availability and theme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Apply status bar styling
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use background color for status bar for edge-to-edge feeling
            window.statusBarColor = colorScheme.background.toArgb()
            // Set proper light/dark appearance for status bar icons
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Also set navigation bar color
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}