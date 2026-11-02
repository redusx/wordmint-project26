package com.example.wordstore.ui.components

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.wordstore.R
import com.example.wordstore.ui.theme.WordMintTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeDialog(hour: Int, minute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val colors = WordMintTheme.colors
    val state = rememberTimePickerState(hour, minute, DateFormat.is24HourFormat(LocalContext.current))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.titleLarge) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.surfaceSunken,
                    selectorColor = colors.primary,
                    timeSelectorSelectedContainerColor = colors.surfaceSunken,
                    timeSelectorSelectedContentColor = colors.primary,
                    timeSelectorUnselectedContainerColor = colors.surfaceSunken,
                    timeSelectorUnselectedContentColor = colors.ink,
                    periodSelectorSelectedContainerColor = colors.surfaceSunken,
                    periodSelectorSelectedContentColor = colors.primary
                )
            )
        },
        confirmButton = { QuietButton(stringResource(R.string.action_save_changes), { onConfirm(state.hour, state.minute) }) },
        dismissButton = { QuietButton(stringResource(R.string.action_cancel), onDismiss, contentColor = colors.inkSecondary) }
    )
}
