package com.example.wordstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.wordstore.domain.repository.ThemeMode

private val LocalWordMintColors = staticCompositionLocalOf { LightWordMintColors }
private val LocalExtraType = staticCompositionLocalOf { WordMintExtraType() }

object WordMintTheme {
    val colors: WordMintColors
        @Composable @ReadOnlyComposable get() = LocalWordMintColors.current

    val type: WordMintExtraType
        @Composable @ReadOnlyComposable get() = LocalExtraType.current
}

/** Dynamic color is off on purpose: copper and verdigris carry meaning. */
@Composable
fun WordMintTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkWordMintColors else LightWordMintColors
    val scheme = remember(colors) { materialScheme(colors) }

    CompositionLocalProvider(LocalWordMintColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = WordMintTypography,
            shapes = WordMintShapes,
            content = content
        )
    }
}

private fun materialScheme(c: WordMintColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.surfaceSunken,
        onPrimaryContainer = c.ink,
        secondary = c.copper,
        onSecondary = c.onPrimary,
        secondaryContainer = c.surfaceSunken,
        onSecondaryContainer = c.ink,
        tertiary = c.sea,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.surfaceSunken,
        onTertiaryContainer = c.ink,
        background = c.canvas,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        surfaceVariant = c.surfaceSunken,
        onSurfaceVariant = c.inkSecondary,
        surfaceContainerLowest = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surfaceSunken,
        surfaceTint = Color.Transparent,
        outline = c.inkMuted,
        outlineVariant = c.hairline,
        error = c.danger,
        onError = c.onPrimary
    )
} else {
    lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.surfaceSunken,
        onPrimaryContainer = c.ink,
        secondary = c.copper,
        onSecondary = c.onPrimary,
        secondaryContainer = c.surfaceSunken,
        onSecondaryContainer = c.ink,
        tertiary = c.sea,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.surfaceSunken,
        onTertiaryContainer = c.ink,
        background = c.canvas,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        surfaceVariant = c.surfaceSunken,
        onSurfaceVariant = c.inkSecondary,
        surfaceContainerLowest = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surfaceSunken,
        surfaceTint = Color.Transparent,
        outline = c.inkMuted,
        outlineVariant = c.hairline,
        error = c.danger,
        onError = c.onPrimary
    )
}
