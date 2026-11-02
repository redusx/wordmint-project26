package com.example.wordstore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.wordstore.R

/** Scholarly and built for linguistics, with full IPA, Cyrillic and Turkish coverage. */
val Gentium = FontFamily(
    Font(R.font.gentium_book_regular, FontWeight.Normal),
    Font(R.font.gentium_book_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.gentium_book_bold, FontWeight.Bold)
)

val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold)
)

private fun serif(size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Gentium,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

private fun sans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp
)

val WordMintTypography = Typography(
    displayLarge = serif(44, 50, -0.4),
    displayMedium = serif(34, 40, -0.2),
    displaySmall = serif(30, 36, -0.2),
    headlineLarge = serif(30, 36, -0.2),
    headlineMedium = serif(28, 34, -0.1),
    headlineSmall = serif(24, 30),
    titleLarge = serif(22, 28),
    titleMedium = serif(20, 26),
    titleSmall = sans(15, 20, FontWeight.Medium),
    bodyLarge = serif(17, 26),
    bodyMedium = sans(15, 22),
    bodySmall = sans(13, 18),
    labelLarge = sans(15, 20, FontWeight.SemiBold),
    labelMedium = sans(13, 18, FontWeight.Medium),
    labelSmall = sans(11, 14, FontWeight.Medium)
)

@Immutable
data class WordMintExtraType(
    val phonetic: TextStyle = serif(17, 24),
    val partOfSpeech: TextStyle = serif(16, 22).copy(fontStyle = FontStyle.Italic),
    val example: TextStyle = serif(17, 25).copy(fontStyle = FontStyle.Italic),
    val figure: TextStyle = sans(22, 28, FontWeight.Medium).copy(
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    ),
    val figureLarge: TextStyle = serif(44, 50, -0.4)
)
