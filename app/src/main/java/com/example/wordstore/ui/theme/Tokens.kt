package com.example.wordstore.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val huge = 64.dp

    /** Room a screen must leave below its content for the floating navigation bar. */
    val navigationClearance = 104.dp
}

object Radii {
    val s = RoundedCornerShape(10.dp)
    val m = RoundedCornerShape(16.dp)
    val l = RoundedCornerShape(24.dp)
    val xl = RoundedCornerShape(32.dp)

    /** A sheet that rises from the bottom edge. */
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    /** A glass header that runs off the top of the screen. */
    val header = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
}

val WordMintShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = Radii.s,
    medium = Radii.m,
    large = Radii.l,
    extraLarge = Radii.xl
)

object Sizes {
    val touchTarget = 48.dp
    val button = 56.dp
    val ratingButton = 76.dp
    val icon = 24.dp
    val iconSmall = 20.dp
}

object Motion {
    const val QUICK = 140
    const val STANDARD = 280

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** The tube's liquid and the navigation lens: slow to settle, with a little give. */
    fun <T> liquid() = spring<T>(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)
}
