package com.example.wordstore.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordstore.R
import com.example.wordstore.data.repository.SharedPrefsSettingsRepository
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.WordMintTheme

@Composable
fun GoalStepper(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = WordMintTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconAction(
            AppIcons.Minus,
            stringResource(R.string.settings_goal_decrease),
            { onChange((value - STEP).coerceAtLeast(0)) },
            enabled = value > 0
        )
        Text(
            value.toString(),
            style = WordMintTheme.type.figure,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .padding(horizontal = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        IconAction(
            AppIcons.Plus,
            stringResource(R.string.settings_goal_increase),
            { onChange((value + STEP).coerceAtMost(SharedPrefsSettingsRepository.MAX_GOAL)) },
            enabled = value < SharedPrefsSettingsRepository.MAX_GOAL
        )
    }
}

private const val STEP = 5
