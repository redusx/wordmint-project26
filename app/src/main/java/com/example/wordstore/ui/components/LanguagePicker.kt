package com.example.wordstore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.wordstore.R
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.ui.format.languageName
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme

/** Every language ML Kit offers. Downloaded packs come first, then the rest by name. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    title: String,
    packs: List<LanguagePack>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = WordMintTheme.colors
    val sorted = remember(packs) {
        packs.sortedWith(compareBy({ !it.isDownloaded }, { languageName(it.code) }))
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = colors.surface,
        shape = Radii.sheet
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                modifier = Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.m)
            )
            Hairline()
            LazyColumn {
                items(sorted, key = { it.code }) { pack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(pack.code) }
                            .heightIn(min = 56.dp)
                            .padding(horizontal = Spacing.xl, vertical = Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.m)
                    ) {
                        Text(
                            text = languageName(pack.code),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (pack.code == selected) colors.primary else colors.ink,
                            modifier = Modifier.weight(1f)
                        )
                        when {
                            pack.isDownloading -> CircularProgressIndicator(
                                Modifier.size(Sizes.iconSmall),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                            pack.isDownloaded -> Text(
                                stringResource(R.string.pack_on_device),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted
                            )
                            else -> Icon(
                                AppIcons.Download,
                                contentDescription = stringResource(R.string.pack_needs_download),
                                tint = colors.inkMuted,
                                modifier = Modifier.size(Sizes.iconSmall)
                            )
                        }
                        if (pack.code == selected) {
                            Icon(
                                AppIcons.Check,
                                contentDescription = stringResource(R.string.selected),
                                tint = colors.primary,
                                modifier = Modifier.size(Sizes.iconSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Language packs are about 30 MB, so using mobile data is always the user's explicit choice. */
@Composable
fun CellularDownloadDialog(language: String, onConfirm: () -> Unit, onWaitForWifi: () -> Unit, onDismiss: () -> Unit) {
    val colors = WordMintTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.pack_cellular_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(R.string.pack_cellular_body, language), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { QuietButton(stringResource(R.string.pack_cellular_confirm), onConfirm) },
        dismissButton = { QuietButton(stringResource(R.string.pack_cellular_wait), onWaitForWifi, contentColor = colors.inkSecondary) }
    )
}

/** Picks a language, downloads its pack if needed, and asks before using mobile data. */
@Composable
fun rememberPackDownloader(
    isUnmetered: () -> Boolean,
    download: (code: String, allowCellular: Boolean) -> Unit
): (String) -> Unit {
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    pending?.let { code ->
        CellularDownloadDialog(
            language = languageName(code),
            onConfirm = {
                download(code, true)
                pending = null
            },
            onWaitForWifi = {
                download(code, false)
                pending = null
            },
            onDismiss = { pending = null }
        )
    }
    return { code -> if (isUnmetered()) download(code, false) else pending = code }
}
