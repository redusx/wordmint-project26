package com.example.wordstore.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * AntigravityAgentCard - A VisionOS/Spatial Design inspired "Liquid Glass" card
 * with levitating animation and volumetric shadows.
 *
 * Design Language: Apple VisionOS / Spatial Design
 * Effects: Frosted Glass, Specular Rim Light, Antigravity Levitation, Volumetric Glow
 */

// ====================
// Color Palette
// ====================
object GlassColors {
    // Neon Agent Colors
    val NeonCyan = Color(0xFF00D4FF)
    val NeonCyanDark = Color(0xFF00A8CC)
    val NeonPurple = Color(0xFFB388FF)
    val NeonMagenta = Color(0xFFFF00FF)
    
    // Glass Surface Colors (Light Theme)
    val GlassSurfaceLight = Color.White.copy(alpha = 0.12f)
    val GlassHighlightLight = Color.White.copy(alpha = 0.25f)
    val GlassBorderTopLight = Color.White.copy(alpha = 0.5f)
    val GlassBorderBottomLight = Color.White.copy(alpha = 0.05f)
    
    // Glass Surface Colors (Dark Theme)
    val GlassSurfaceDark = Color.White.copy(alpha = 0.08f)
    val GlassHighlightDark = Color.White.copy(alpha = 0.15f)
    val GlassBorderTopDark = Color.White.copy(alpha = 0.35f)
    val GlassBorderBottomDark = Color.Transparent
    
    // Shadow/Glow Colors
    val ShadowCyan = Color(0x4000D4FF)
    val AmbientGlow = Color(0x2000D4FF)
}

// ====================
// Main Component
// ====================
@Composable
fun AntigravityAgentCard(
    modifier: Modifier = Modifier,
    agentName: String = "Agent",
    agentStatus: String = "Active",
    accentColor: Color = GlassColors.NeonCyan,
    onClick: (() -> Unit)? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    // ==================
    // Antigravity Animation (Levitation)
    // ==================
    val infiniteTransition = rememberInfiniteTransition(label = "levitation")
    
    // Vertical float animation
    val translationY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    
    // Subtle rotation for organic feel
    val rotationZ by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3500,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotateZ"
    )
    
    // Glow pulse animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    // Orb rotation animation
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbRotation"
    )

    // ==================
    // Glass Colors based on theme
    // ==================
    val glassSurface = if (isDarkTheme) GlassColors.GlassSurfaceDark else GlassColors.GlassSurfaceLight
    val glassHighlight = if (isDarkTheme) GlassColors.GlassHighlightDark else GlassColors.GlassHighlightLight
    val borderTop = if (isDarkTheme) GlassColors.GlassBorderTopDark else GlassColors.GlassBorderTopLight
    val borderBottom = if (isDarkTheme) GlassColors.GlassBorderBottomDark else GlassColors.GlassBorderBottomLight

    // ==================
    // Card Shape
    // ==================
    val cardShape = RoundedCornerShape(24.dp)

    // ==================
    // Volumetric Shadow + Levitating Card
    // ==================
    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = translationY
                this.rotationZ = rotationZ
            }
    ) {
        // Volumetric Glow Shadow Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 12.dp)
                .blur(24.dp)
                .clip(cardShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowAlpha * 0.4f),
                            accentColor.copy(alpha = glowAlpha * 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Glass Card
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = cardShape,
                    spotColor = accentColor.copy(alpha = 0.3f),
                    ambientColor = GlassColors.AmbientGlow
                )
                // Specular Rim Light Border
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            borderTop,
                            borderBottom
                        )
                    ),
                    shape = cardShape
                ),
            shape = cardShape,
            color = Color.Transparent
        ) {
            // Frosted Glass Background
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
                    .applyBlurEffect()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Agent Orb
                    AgentOrb(
                        size = 64.dp,
                        accentColor = accentColor,
                        glowAlpha = glowAlpha,
                        rotation = orbRotation
                    )
                    
                    // Agent Info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = agentName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status Indicator Dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = agentStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = accentColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Activity Indicator
                    ActivityWave(
                        accentColor = accentColor,
                        glowAlpha = glowAlpha
                    )
                }
            }
        }
    }
}

// ====================
// Agent Orb Component
// ====================
@Composable
private fun AgentOrb(
    size: Dp,
    accentColor: Color,
    glowAlpha: Float,
    rotation: Float
) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = Offset(sizePx / 2, sizePx / 2)
        val radius = sizePx / 2
        
        // Outer Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = glowAlpha * 0.5f),
                    accentColor.copy(alpha = glowAlpha * 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.5f
            ),
            radius = radius * 1.5f,
            center = center
        )
        
        // Core Orb with Radial Gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    accentColor,
                    accentColor.copy(alpha = 0.7f)
                ),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                radius = radius * 1.2f
            ),
            radius = radius * 0.7f,
            center = center
        )
        
        // Specular Highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                radius = radius * 0.3f
            ),
            radius = radius * 0.3f,
            center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f)
        )
        
        // Orbiting Particles
        val particleCount = 3
        for (i in 0 until particleCount) {
            val angle = Math.toRadians((rotation + i * (360f / particleCount)).toDouble())
            val orbitRadius = radius * 0.85f
            val particleX = center.x + orbitRadius * cos(angle).toFloat()
            val particleY = center.y + orbitRadius * sin(angle).toFloat()
            
            drawCircle(
                color = accentColor.copy(alpha = glowAlpha),
                radius = 3.dp.toPx(),
                center = Offset(particleX, particleY)
            )
        }
    }
}

// ====================
// Activity Wave Component
// ====================
@Composable
private fun ActivityWave(
    accentColor: Color,
    glowAlpha: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val delay = index * 150
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveBar$index"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = glowAlpha),
                                accentColor.copy(alpha = glowAlpha * 0.5f)
                            )
                        )
                    )
            )
        }
    }
}

// ====================
// Blur Effect Extension
// ====================
@Composable
private fun Modifier.applyBlurEffect(): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = 16.dp)
    } else {
        // Fallback scrim for older APIs
        this.drawBehind {
            drawRect(
                color = Color.Black.copy(alpha = 0.1f)
            )
        }
    }
}

// ====================
// Preview / Demo
// ====================
@Composable
fun AntigravityAgentCardDemo() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        Color(0xFF0D1B2A)
                    )
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AntigravityAgentCard(
            modifier = Modifier.fillMaxWidth(),
            agentName = "Antigravity",
            agentStatus = "Processing...",
            accentColor = GlassColors.NeonCyan
        )
        
        AntigravityAgentCard(
            modifier = Modifier.fillMaxWidth(),
            agentName = "Claude Agent",
            agentStatus = "Active",
            accentColor = GlassColors.NeonPurple
        )
        
        AntigravityAgentCard(
            modifier = Modifier.fillMaxWidth(),
            agentName = "Vision Agent",
            agentStatus = "Standby",
            accentColor = GlassColors.NeonMagenta
        )
    }
}
