package com.example.wordstore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.AnimationSpecs
import com.example.wordstore.ui.theme.BrandColors
import com.example.wordstore.ui.theme.CornerRadius
import com.example.wordstore.ui.theme.Elevation
import com.example.wordstore.ui.theme.Gradients
import com.example.wordstore.ui.theme.IconSize
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordStoreIcons
import kotlinx.coroutines.delay

/**
 * Animated list item wrapper that provides fade-in and slide-up animation for items in a LazyColumn
 * with enhanced motion design.
 *
 * @param visible Whether the item should be visible
 * @param index Index of the item for staggered animation delay
 * @param content The composable content to animate
 */
@Composable
fun AnimatedListItem(visible: Boolean = true, index: Int = 0, content: @Composable () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            // Stagger animation based on index (max 8 items staggered)
            delay((index.coerceAtMost(8) * 40).toLong())
            isVisible = true
        } else {
            isVisible = false
        }
    }

    AnimatedVisibility(
            visible = isVisible,
            enter =
                    fadeIn(
                            animationSpec =
                                    tween(
                                            AnimationSpecs.StandardDuration,
                                            easing = FastOutSlowInEasing
                                    )
                    ) +
                            slideInVertically(
                                    animationSpec =
                                            tween(
                                                    AnimationSpecs.StandardDuration,
                                                    easing = FastOutSlowInEasing
                                            ),
                                    initialOffsetY = { it / 3 }
                            ) +
                            scaleIn(
                                    animationSpec = tween(AnimationSpecs.StandardDuration),
                                    initialScale = 0.95f
                            ),
            exit =
                    fadeOut(animationSpec = tween(AnimationSpecs.ShortDuration)) +
                            slideOutVertically(
                                    animationSpec = tween(AnimationSpecs.ShortDuration),
                                    targetOffsetY = { -it / 4 }
                            )
    ) { content() }
}

/**
 * Premium empty state composable with icon, title, and description. Features elegant animation and
 * modern styling.
 *
 * @param icon The icon to display
 * @param title The main title text
 * @param description The secondary description text
 * @param actionText Optional action button text
 * @param onAction Optional callback for action button
 * @param modifier Optional modifier
 */
@Composable
fun EmptyState(
        icon: ImageVector,
        title: String,
        description: String,
        actionText: String? = null,
        onAction: (() -> Unit)? = null,
        modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        isVisible = true
    }

    val alpha by
            animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec =
                            tween(AnimationSpecs.LongDuration, easing = FastOutSlowInEasing),
                    label = "empty_state_alpha"
            )

    val scale by
            animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0.85f,
                    animationSpec =
                            tween(AnimationSpecs.LongDuration, easing = FastOutSlowInEasing),
                    label = "empty_state_scale"
            )

    val translateY by
            animateFloatAsState(
                    targetValue = if (isVisible) 0f else 30f,
                    animationSpec =
                            tween(AnimationSpecs.LongDuration, easing = FastOutSlowInEasing),
                    label = "empty_state_translate"
            )

    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(Spacing.XL)
                            .graphicsLayer {
                                this.alpha = alpha
                                scaleX = scale
                                scaleY = scale
                                this.translationY = translateY
                            }
                            .semantics { contentDescription = "$title. $description" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        // Icon container with gradient background
        Box(
                modifier =
                        Modifier.size(96.dp)
                                .clip(CircleShape)
                                .background(
                                        brush =
                                                Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        MaterialTheme.colorScheme
                                                                                .primaryContainer
                                                                                .copy(alpha = 0.8f),
                                                                        MaterialTheme.colorScheme
                                                                                .primaryContainer
                                                                                .copy(alpha = 0.3f)
                                                                )
                                                )
                                ),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.Feature),
                    tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.L))

        Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.S))

        Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.L)
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(Spacing.L))
            FilledTonalButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = Spacing.L, vertical = Spacing.MS)
            ) { Text(actionText) }
        }
    }
}

/**
 * Premium shimmer loading placeholder effect. Creates a smooth animated shimmer to indicate loading
 * state.
 *
 * @param modifier Modifier for sizing and positioning
 * @param shape Shape of the placeholder
 */
@Composable
fun ShimmerPlaceholder(
        modifier: Modifier = Modifier,
        shape: Shape = RoundedCornerShape(Spacing.S)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by
            infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation =
                                            tween(
                                                    durationMillis = 1200,
                                                    easing = FastOutSlowInEasing
                                            ),
                                    repeatMode = RepeatMode.Restart
                            ),
                    label = "shimmer_progress"
            )

    val shimmerColors =
            listOf(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
            )

    Box(
            modifier =
                    modifier.clip(shape)
                            .background(
                                    brush =
                                            Brush.linearGradient(
                                                    colors = shimmerColors,
                                                    start =
                                                            Offset(
                                                                    shimmerProgress * 400f - 200f,
                                                                    0f
                                                            ),
                                                    end = Offset(shimmerProgress * 400f + 200f, 0f)
                                            )
                            )
    )
}

/**
 * Loading card placeholder with enhanced shimmer effect. Use as a placeholder while loading word
 * cards.
 */
@Composable
fun LoadingWordCard(modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.Large),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Level1)
    ) {
        Column(modifier = Modifier.padding(Spacing.L)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.6f).height(24.dp))

                    Spacer(modifier = Modifier.height(Spacing.S))

                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp))
                }

                ShimmerPlaceholder(modifier = Modifier.size(40.dp), shape = CircleShape)
            }
        }
    }
}

/** Multiple loading cards for list loading state. */
@Composable
fun LoadingWordList(itemCount: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
        repeat(itemCount) { LoadingWordCard() }
    }
}

/** Premium glass button - Liquid Glass style (gray only). */
@Composable
fun GradientButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        enabled: Boolean = true,
        isLoading: Boolean = false
) {
    val buttonScale by
            animateFloatAsState(
                    targetValue = if (enabled && !isLoading) 1f else 0.98f,
                    animationSpec = tween(AnimationSpecs.ShortDuration),
                    label = "button_scale"
            )

    val buttonShape = RoundedCornerShape(16.dp)

    Button(
            onClick = onClick,
            modifier = modifier.height(56.dp).scale(buttonScale),
            enabled = enabled && !isLoading,
            shape = buttonShape,
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                    ),
            contentPadding = PaddingValues()
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(56.dp)
                                .border(
                                        width = 1.dp,
                                        color =
                                                if (enabled) Color.White.copy(alpha = 0.15f)
                                                else Color.White.copy(alpha = 0.05f),
                                        shape = buttonShape
                                )
                                .background(
                                        color =
                                                if (enabled && !isLoading)
                                                        Color.White.copy(alpha = 0.1f)
                                                else Color.White.copy(alpha = 0.03f),
                                        shape = buttonShape
                                ),
                contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White.copy(alpha = 0.7f),
                        strokeWidth = 2.dp
                )
            } else {
                Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(Spacing.S))
                    }
                    Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

/** Outlined premium button with icon support. */
@Composable
fun OutlinedPremiumButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        enabled: Boolean = true
) {
    FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(CornerRadius.Large),
            colors =
                    ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
    ) {
        Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.S))
            }
            Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Section header with optional action. */
@Composable
fun SectionHeader(
        title: String,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
) {
    Row(
            modifier = modifier.fillMaxWidth().padding(vertical = Spacing.S),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSize.MediumSmall)
                )
                Spacer(modifier = Modifier.width(Spacing.S))
            }
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Status badge/chip component. */
@Composable
fun StatusBadge(
        text: String,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        type: BadgeType = BadgeType.DEFAULT
) {
    val (backgroundColor, contentColor) =
            when (type) {
                BadgeType.SUCCESS -> Pair(BrandColors.SuccessLight, BrandColors.Success)
                BadgeType.WARNING -> Pair(BrandColors.WarningLight, BrandColors.Warning)
                BadgeType.ERROR ->
                        Pair(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.error
                        )
                BadgeType.INFO -> Pair(BrandColors.InfoLight, BrandColors.Info)
                BadgeType.DEFAULT ->
                        Pair(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                BadgeType.PRIMARY ->
                        Pair(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
            }

    Surface(
            modifier = modifier,
            shape = RoundedCornerShape(CornerRadius.Full),
            color = backgroundColor
    ) {
        Row(
                modifier = Modifier.padding(horizontal = Spacing.MS, vertical = Spacing.XS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
        ) {
            if (icon != null) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Small),
                        tint = contentColor
                )
            }
            Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
            )
        }
    }
}

enum class BadgeType {
    DEFAULT,
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

/** Floating info card for tips and suggestions. */
@Composable
fun InfoCard(
        title: String,
        description: String,
        modifier: Modifier = Modifier,
        icon: ImageVector = WordStoreIcons.TipsAndUpdates,
        type: InfoCardType = InfoCardType.TIP
) {
    val (backgroundColor, iconColor) =
            when (type) {
                InfoCardType.TIP ->
                        Pair(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiary
                        )
                InfoCardType.INFO ->
                        Pair(BrandColors.InfoLight.copy(alpha = 0.5f), BrandColors.Info)
                InfoCardType.WARNING ->
                        Pair(BrandColors.WarningLight.copy(alpha = 0.5f), BrandColors.Warning)
            }

    Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.Medium),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(Spacing.M), verticalAlignment = Alignment.Top) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(IconSize.Medium)
            )
            Spacer(modifier = Modifier.width(Spacing.MS))
            Column {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.XS))
                Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class InfoCardType {
    TIP,
    INFO,
    WARNING
}

/** Animated progress indicator for translation operations. */
@Composable
fun TranslationProgress(modifier: Modifier = Modifier, text: String = "Çeviri yapılıyor...") {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(CornerRadius.Full)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
        )
        Spacer(modifier = Modifier.height(Spacing.S))
        Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Decorative header with gradient and icon. */
@Composable
fun ScreenHeader(
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(Spacing.L)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(RoundedCornerShape(CornerRadius.Medium))
                                        .background(brush = Gradients.Primary),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(IconSize.Medium)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.M))
            }
            Column {
                Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Pulse animation wrapper for drawing attention to elements. */
@Composable
fun PulseAnimation(enabled: Boolean = true, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by
            infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (enabled) 1.05f else 1f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                            ),
                    label = "pulse_scale"
            )

    Box(modifier = Modifier.scale(scale)) { content() }
}
