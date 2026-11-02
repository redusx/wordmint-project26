package com.example.wordstore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordstore.ui.theme.Motion
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import dev.chrisbanes.haze.HazeState

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

/** Floating capsule of glass. A lighter lens slides to the selected destination. */
@Composable
fun GlassNavigationBar(
    items: List<NavigationItem>,
    selectedRoute: String?,
    hazeState: HazeState,
    onSelect: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WordMintTheme.colors
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.l, vertical = Spacing.s)
    ) {
        GlassSurface(
            hazeState = hazeState,
            shape = CircleShape,
            // Grows with the system font size, so labels are never clipped.
            modifier = Modifier.fillMaxWidth().height(54.dp + with(LocalDensity.current) { 14.sp.toDp() })
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight().padding(Spacing.xs)) {
                val itemWidth = maxWidth / items.size
                val lensOffset by animateDpAsState(itemWidth * selectedIndex, Motion.liquid(), label = "nav_lens")
                Box(
                    Modifier
                        .offset { IntOffset(lensOffset.roundToPx(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colors.tubeHighlight.copy(alpha = if (colors.isDark) 0.16f else 0.7f))
                        .border(1.dp, colors.glassRimTop, CircleShape)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    items.forEachIndexed { index, item ->
                        NavigationEntry(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(item) },
                            modifier = Modifier.width(itemWidth).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationEntry(
    item: NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = WordMintTheme.colors
    val tint by animateColorAsState(
        if (selected) colors.primary else colors.inkSecondary,
        tween(Motion.QUICK),
        label = "nav_tint"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xxs)
        )
    }
}
