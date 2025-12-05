package com.example.wordstore.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for consistent layout across the app.
 * Use these instead of hardcoded dp values for maintainability.
 */
object Spacing {
    /** 2dp - Micro spacing */
    val XXS: Dp = 2.dp
    
    /** 4dp - Innermost padding, icon margins */
    val XS: Dp = 4.dp
    
    /** 8dp - Between related items, small gaps */
    val S: Dp = 8.dp
    
    /** 12dp - Medium-small gaps */
    val MS: Dp = 12.dp
    
    /** 16dp - Section padding, list item padding */
    val M: Dp = 16.dp
    
    /** 20dp - Medium-large gaps */
    val ML: Dp = 20.dp
    
    /** 24dp - Screen padding, major section gaps */
    val L: Dp = 24.dp
    
    /** 32dp - Large gaps between major sections */
    val XL: Dp = 32.dp
    
    /** 40dp - Extra large gaps */
    val XXL: Dp = 40.dp
    
    /** 48dp - Major section separation */
    val XXXL: Dp = 48.dp
    
    /** 56dp - Hero section spacing */
    val Hero: Dp = 56.dp
}

/**
 * Elevation tokens following Material 3 guidelines.
 * Higher elevation = more prominence.
 */
object Elevation {
    /** 0dp - Flat surfaces, backgrounds */
    val Level0: Dp = 0.dp
    
    /** 1dp - Cards at rest */
    val Level1: Dp = 1.dp
    
    /** 2dp - Slight elevation for interactive states */
    val Level2: Dp = 2.dp
    
    /** 4dp - Cards on hover/focus */
    val Level3: Dp = 4.dp
    
    /** 6dp - Dialogs, modal bottom sheets */
    val Level4: Dp = 6.dp
    
    /** 8dp - FAB, pressed states */
    val Level5: Dp = 8.dp
    
    /** 12dp - Navigation drawer, side sheet */
    val Level6: Dp = 12.dp
}

/**
 * Touch target sizes for accessibility compliance.
 * All interactive elements should meet minimum touch target.
 */
object TouchTarget {
    /** 44dp - Minimum touch target */
    val Minimum: Dp = 44.dp
    
    /** 48dp - Standard minimum per Material guidelines */
    val Standard: Dp = 48.dp
    
    /** 56dp - Standard button height */
    val Button: Dp = 56.dp
    
    /** 64dp - Large buttons, FAB */
    val Large: Dp = 64.dp
    
    /** 72dp - Extra large interactive areas */
    val XLarge: Dp = 72.dp
}

/**
 * Icon sizes for consistency.
 */
object IconSize {
    /** 12dp - Micro icons, indicators */
    val XSmall: Dp = 12.dp
    
    /** 16dp - Small inline icons */
    val Small: Dp = 16.dp
    
    /** 20dp - Standard small icons */
    val MediumSmall: Dp = 20.dp
    
    /** 24dp - Standard icons (default) */
    val Medium: Dp = 24.dp
    
    /** 28dp - Medium-large icons */
    val MediumLarge: Dp = 28.dp
    
    /** 32dp - Large icons */
    val Large: Dp = 32.dp
    
    /** 40dp - XLarge icons */
    val XLarge: Dp = 40.dp
    
    /** 48dp - Hero icons */
    val XXLarge: Dp = 48.dp
    
    /** 64dp - Feature icons */
    val Feature: Dp = 64.dp
    
    /** 96dp - Illustration icons */
    val Illustration: Dp = 96.dp
}

/**
 * Corner radius values for shapes.
 */
object CornerRadius {
    /** 4dp - Chips, badges */
    val ExtraSmall: Dp = 4.dp
    
    /** 8dp - Buttons, small cards */
    val Small: Dp = 8.dp
    
    /** 12dp - Cards, text fields */
    val Medium: Dp = 12.dp
    
    /** 16dp - Large cards, dialogs */
    val Large: Dp = 16.dp
    
    /** 20dp - Extra large cards */
    val XLarge: Dp = 20.dp
    
    /** 24dp - Full dialogs */
    val XXLarge: Dp = 24.dp
    
    /** 28dp - Bottom sheets */
    val ExtraLarge: Dp = 28.dp
    
    /** 9999dp - Full rounded (circle/pill) */
    val Full: Dp = 9999.dp
}

/**
 * Content sizing for cards and containers
 */
object ContentSize {
    /** Standard card height */
    val CardHeight: Dp = 80.dp
    
    /** Compact card height */
    val CardCompact: Dp = 64.dp
    
    /** Large card height */
    val CardLarge: Dp = 120.dp
    
    /** Header height */
    val HeaderHeight: Dp = 64.dp
    
    /** Bottom bar height */
    val BottomBarHeight: Dp = 80.dp
    
    /** FAB size */
    val FabSize: Dp = 56.dp
    
    /** Large FAB size */
    val FabLarge: Dp = 96.dp
    
    /** Avatar small */
    val AvatarSmall: Dp = 32.dp
    
    /** Avatar medium */
    val AvatarMedium: Dp = 48.dp
    
    /** Avatar large */
    val AvatarLarge: Dp = 64.dp
    
    /** Thumbnail size */
    val Thumbnail: Dp = 72.dp
    
    /** Dialog max width */
    val DialogMaxWidth: Dp = 400.dp
    
    /** Max content width for tablets */
    val MaxContentWidth: Dp = 600.dp
}

/**
 * Border widths for outlined elements
 */
object BorderWidth {
    /** 0.5dp - Subtle dividers */
    val Hairline: Dp = 0.5.dp
    
    /** 1dp - Standard borders */
    val Thin: Dp = 1.dp
    
    /** 1.5dp - Medium borders */
    val Medium: Dp = 1.5.dp
    
    /** 2dp - Thick borders, focused states */
    val Thick: Dp = 2.dp
    
    /** 3dp - Extra thick, selected states */
    val ExtraThick: Dp = 3.dp
}
