package com.example.wordstore.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Patina palette. A word ages like a minted coin: new words are copper, mastered words are
 * verdigris, and bronze sits between them. See docs/DESIGN_SYSTEM.md.
 */
@Immutable
data class WordMintColors(
    val isDark: Boolean,
    val canvas: Color,
    val canvasDeep: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val hairline: Color,
    val primary: Color,
    val onPrimary: Color,
    val copper: Color,
    val bronze: Color,
    val verdigris: Color,
    val sea: Color,
    val danger: Color,
    /** Marks only, not text: separated by lightness as well as hue, so they survive color blindness. */
    val statusNew: Color,
    val statusLearning: Color,
    val statusMastered: Color,
    val glassTint: Color,
    val glassTintOpaque: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val glassShadow: Color,
    val tubeGlass: Color,
    val tubeRim: Color,
    val tubeHighlight: Color,
    val liquid: Color,
    val liquidDeep: Color
)

val LightWordMintColors = WordMintColors(
    isDark = false,
    canvas = Color(0xFFEDF1EF),
    canvasDeep = Color(0xFFE4EBE7),
    surface = Color(0xFFFBFCFB),
    surfaceSunken = Color(0xFFE3E9E6),
    ink = Color(0xFF10211E),
    inkSecondary = Color(0xFF3F514B),
    inkMuted = Color(0xFF586962),
    hairline = Color(0x1F10211E),
    primary = Color(0xFF0E6A5B),
    onPrimary = Color(0xFFFFFFFF),
    copper = Color(0xFFA2522A),
    bronze = Color(0xFF7C6C1F),
    verdigris = Color(0xFF14796A),
    sea = Color(0xFF1C6488),
    danger = Color(0xFFA8321F),
    statusNew = Color(0xFFA2522A),
    statusLearning = Color(0xFFC9A227),
    statusMastered = Color(0xFF14796A),
    glassTint = Color(0x99F6F9F7),
    glassTintOpaque = Color(0xF0F6F9F7),
    glassRimTop = Color(0xE6FFFFFF),
    glassRimBottom = Color(0x1410211E),
    glassShadow = Color(0x2E0B2A24),
    tubeGlass = Color(0x66FFFFFF),
    tubeRim = Color(0x4010211E),
    tubeHighlight = Color(0xCCFFFFFF),
    liquid = Color(0xFF2B9C87),
    liquidDeep = Color(0xFF14796A)
)

val DarkWordMintColors = WordMintColors(
    isDark = true,
    canvas = Color(0xFF0D1A18),
    canvasDeep = Color(0xFF091312),
    surface = Color(0xFF162523),
    surfaceSunken = Color(0xFF10201D),
    ink = Color(0xFFE9F1EE),
    inkSecondary = Color(0xFFAFC0BA),
    inkMuted = Color(0xFF8FA199),
    hairline = Color(0x1AFFFFFF),
    primary = Color(0xFF63CDB8),
    onPrimary = Color(0xFF06211C),
    copper = Color(0xFFE5A67F),
    bronze = Color(0xFFCDB866),
    verdigris = Color(0xFF63CDB8),
    sea = Color(0xFF7FC3E0),
    danger = Color(0xFFF0907C),
    statusNew = Color(0xFFD9804A),
    statusLearning = Color(0xFFE8CF73),
    statusMastered = Color(0xFF3DB39D),
    glassTint = Color(0x8C1C2F2C),
    glassTintOpaque = Color(0xF21C2F2C),
    glassRimTop = Color(0x38FFFFFF),
    glassRimBottom = Color(0x0AFFFFFF),
    glassShadow = Color(0x66000000),
    tubeGlass = Color(0x14FFFFFF),
    tubeRim = Color(0x40FFFFFF),
    tubeHighlight = Color(0x40FFFFFF),
    liquid = Color(0xFF63CDB8),
    liquidDeep = Color(0xFF2B8F7D)
)
