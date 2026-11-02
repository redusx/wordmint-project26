package com.example.wordstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.wordstore.R
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    val colors = WordMintTheme.colors
    val focus = LocalFocusManager.current
    val text = rememberEchoedText(value)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(colors.surfaceSunken.copy(alpha = 0.72f), Radii.s)
            .padding(start = Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.Search, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(Sizes.iconSmall))
        Box(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
            if (text.value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.titleMedium, color = colors.inkMuted, maxLines = 1)
            }
            BasicTextField(
                value = text.value,
                onValueChange = {
                    text.onEdit(it)
                    onValueChange(it)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focus.clearFocus()
                        onSearch?.invoke()
                    },
                    onDone = { focus.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    // Hardware keyboards send Enter as a key, not as an IME action.
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && onSearch != null) {
                            if (event.type == KeyEventType.KeyUp) {
                                focus.clearFocus()
                                onSearch()
                            }
                            true
                        } else {
                            false
                        }
                    }
                    .semantics { contentDescription = placeholder }
                    .testTag("search_field")
            )
        }
        if (text.value.isNotEmpty()) {
            IconAction(AppIcons.Close, stringResource(R.string.action_clear), onClear, tint = colors.inkSecondary)
        }
    }
}
