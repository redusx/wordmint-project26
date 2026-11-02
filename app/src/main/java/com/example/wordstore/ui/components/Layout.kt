package com.example.wordstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme

/** The one background gradient in the app: a slow tonal ramp of a single hue. */
@Composable
fun CanvasBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = WordMintTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.canvas, colors.canvasDeep))),
        content = content
    )
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = WordMintTheme.colors.ink,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = WordMintTheme.colors.ink,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = WordMintTheme.colors.hairline)
}

/** A label on the left and its value on the right, separated from its neighbours by hairlines. */
@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = WordMintTheme.colors.ink,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WordMintTheme.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = WordMintTheme.colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

/** An empty or failed state: what happened, what to do, and optionally a way to do it. */
@Composable
fun MessageBlock(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = WordMintTheme.colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = WordMintTheme.colors.inkSecondary)
        if (action != null) {
            Box(Modifier.padding(top = Spacing.m)) { action() }
        }
    }
}

/** A short inline notice with an optional action, set on a sunken well rather than a card. */
@Composable
fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = WordMintTheme.colors.inkSecondary,
    action: (@Composable () -> Unit)? = null
) {
    val colors = WordMintTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, com.example.wordstore.ui.theme.Radii.m)
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconSmall))
            }
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary, modifier = Modifier.weight(1f))
        }
        if (action != null) action()
    }
}
