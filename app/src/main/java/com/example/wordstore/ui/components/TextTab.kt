package com.example.wordstore.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme

/** A text choice with an underline, so that selection never depends on color alone. */
@Composable
fun TextTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    role: Role = Role.Tab
) {
    val colors = WordMintTheme.colors
    val underline by animateDpAsState(if (selected) 2.dp else 0.dp, label = "tab_underline")
    Column(
        modifier = modifier
            .heightIn(min = Sizes.touchTarget)
            .clickable(onClick = onClick)
            .semantics {
                this.role = role
                this.selected = selected
            }
            .padding(horizontal = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.primary else colors.inkSecondary,
                maxLines = 1
            )
            if (count != null) {
                Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            }
        }
        Spacer(Modifier.height(Spacing.s))
        Spacer(Modifier.width(28.dp).height(underline).background(colors.primary, Radii.s))
        Spacer(Modifier.height(Spacing.xxs))
    }
}
