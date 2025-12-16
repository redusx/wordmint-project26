package com.example.wordstore.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.GlassColors
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.CornerRadius
import com.example.wordstore.ui.theme.IconSize

/**
 * VisionOS / Liquid Glass UI Components
 */

// ====================
// Glass Card
// ====================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color = GlassColors.Accent,
    enableLevitation: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Levitation animation
    val infiniteTransition = rememberInfiniteTransition(label = "cardLevitation")
    val translationY by infiniteTransition.animateFloat(
        initialValue = if (enableLevitation) -4f else 0f,
        targetValue = if (enableLevitation) 4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "pressScale"
    )
    
    // Glass colors
    val glassSurface = if (isDarkTheme) GlassColors.GlassCardDark else GlassColors.GlassCardLight
    val glassHighlight = if (isDarkTheme) GlassColors.GlassHighlightDark else GlassColors.GlassHighlightLight
    val borderTop = if (isDarkTheme) GlassColors.GlassBorderTopDark else GlassColors.GlassBorderTopLight
    val borderBottom = if (isDarkTheme) GlassColors.GlassBorderBottomDark else GlassColors.GlassBorderBottomLight
    
    val cardShape = RoundedCornerShape(20.dp)
    
    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = translationY
            }
            .scale(scale)
    ) {
        // Volumetric Glow Shadow
        if (enableLevitation) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 8.dp)
                    .blur(16.dp)
                    .clip(cardShape)
                    .background(accentColor.copy(alpha = glowAlpha * 0.2f))
            )
        }
        
        // Main Glass Card
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = cardShape,
                    spotColor = accentColor.copy(alpha = 0.2f),
                    ambientColor = GlassColors.AmbientGlow
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(borderTop, borderBottom)
                    ),
                    shape = cardShape
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                ),
            shape = cardShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                glassHighlight,
                                glassSurface,
                                glassSurface.copy(alpha = glassSurface.alpha * 0.5f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            ) {
                content()
            }
        }
    }
}

// ====================
// Glass Button
// ====================
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    accentColor: Color = GlassColors.Accent
) {
    val isDarkTheme = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (enabled) 0.6f else 0.2f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )
    
    val buttonShape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
    ) {
        // Glow layer
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .blur(12.dp)
                    .clip(buttonShape)
                    .background(accentColor.copy(alpha = glowAlpha * 0.4f))
            )
        }
        
        // Button
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.5f),
                            accentColor.copy(alpha = 0.2f)
                        )
                    ),
                    shape = buttonShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled && !isLoading,
                    onClick = onClick
                ),
            shape = buttonShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (enabled) 0.3f else 0.1f),
                                accentColor.copy(alpha = if (enabled) 0.15f else 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) accentColor else accentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ====================
// Glass FAB - Square Geometry, Pure Gray
// ====================
@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accentColor: Color = GlassColors.Accent
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "fabScale"
    )
    
    val fabShape = RoundedCornerShape(16.dp)  // Square with rounded corners
    
    // FAB - Square geometry, gray glass style
    Surface(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = fabShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = fabShape,
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ====================
// Glass Input Field
// ====================
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    accentColor: Color = GlassColors.Accent
) {
    val isDarkTheme = isSystemInDarkTheme()
    val borderTop = if (isDarkTheme) GlassColors.GlassBorderTopDark else GlassColors.GlassBorderTopLight
    val borderBottom = if (isDarkTheme) GlassColors.GlassBorderBottomDark else GlassColors.GlassBorderBottomLight
    
    val fieldShape = RoundedCornerShape(16.dp)
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(borderTop, borderBottom)
                ),
                shape = fieldShape
            ),
        placeholder = {
            Text(
                text = placeholder,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        } else null,
        singleLine = singleLine,
        shape = fieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.7f),
            unfocusedContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.5f),
            cursorColor = accentColor,
            focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
            unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black
        )
    )
}

// ====================
// Glass Top Bar
// ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    accentColor: Color = GlassColors.Accent
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isDarkTheme) GlassColors.GlassBorderTopDark else GlassColors.GlassBorderTopLight,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ),
        color = if (isDarkTheme) GlassColors.GlassCardDark else GlassColors.GlassCardLight,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
            },
            navigationIcon = { navigationIcon?.invoke() },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}
