package com.example.wordstore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.Motion
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = WordMintTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = Radii.m,
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.m),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceSunken,
            disabledContentColor = colors.inkMuted
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        ButtonContent(text, icon, isLoading, if (isLoading) colors.inkMuted else colors.onPrimary)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentColor: Color = WordMintTheme.colors.ink
) {
    val colors = WordMintTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = Radii.m,
        border = BorderStroke(1.dp, if (enabled) colors.inkMuted.copy(alpha = 0.5f) else colors.hairline),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.m),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = colors.inkMuted
        )
    ) {
        ButtonContent(text, icon, isLoading, if (enabled) contentColor else colors.inkMuted)
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = WordMintTheme.colors.primary
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.touchTarget),
        shape = Radii.s,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = WordMintTheme.colors.inkMuted
        )
    ) {
        ButtonContent(text, icon, false, contentColor)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, isLoading: Boolean, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally)
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(Sizes.iconSmall), color = tint, strokeWidth = 2.dp)
            icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** An icon-only action. [description] is required, because an icon alone says nothing to a screen reader. */
@Composable
fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = WordMintTheme.colors.ink
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, tween(Motion.QUICK), label = "icon_press")
    Box(
        modifier = modifier
            .size(Sizes.touchTarget)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else WordMintTheme.colors.inkMuted,
            modifier = Modifier.size(Sizes.icon)
        )
    }
}
