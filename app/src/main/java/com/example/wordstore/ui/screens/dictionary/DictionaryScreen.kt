package com.example.wordstore.ui.screens.dictionary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.model.DictionaryStatus
import com.example.wordstore.domain.model.WordLookup
import com.example.wordstore.ui.components.GlassSurface
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LanguagePickerSheet
import com.example.wordstore.ui.components.MessageBlock
import com.example.wordstore.ui.components.Notice
import com.example.wordstore.ui.components.PrimaryButton
import com.example.wordstore.ui.components.PronounceButton
import com.example.wordstore.ui.components.QuietButton
import com.example.wordstore.ui.components.ScreenTitle
import com.example.wordstore.ui.components.SearchField
import com.example.wordstore.ui.components.SecondaryButton
import com.example.wordstore.ui.components.WordEntryContent
import com.example.wordstore.ui.components.rememberPackDownloader
import com.example.wordstore.ui.format.languageName
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.DictionaryUiState
import com.example.wordstore.viewmodel.DictionaryViewModel
import com.example.wordstore.viewmodel.LookupState
import com.example.wordstore.viewmodel.appViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

private enum class PickerSide { LEARNING, KNOWN }

@Composable
fun DictionaryScreen(container: AppContainer, onOpenWord: (Long) -> Unit) {
    val viewModel = appViewModel {
        DictionaryViewModel(it.dictionary, it.words, it.settings, it.languagePacks, it.clock)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf<PickerSide?>(null) }
    val download = rememberPackDownloader(viewModel::isOnUnmeteredNetwork, viewModel::downloadPack)

    LaunchedEffect(Unit) { viewModel.refreshPacks() }

    DictionaryContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onClear = viewModel::clear,
        onSwap = viewModel::swapDirection,
        onPickFrom = { picker = if (state.isReversed) PickerSide.KNOWN else PickerSide.LEARNING },
        onPickTo = { picker = if (state.isReversed) PickerSide.LEARNING else PickerSide.KNOWN },
        onDownloadMissing = { state.missingPacks.forEach(download) },
        onTranslationChange = viewModel::onTranslationChange,
        onSave = viewModel::save,
        onOpenWord = onOpenWord
    )

    picker?.let { side ->
        LanguagePickerSheet(
            title = stringResource(if (side == PickerSide.LEARNING) R.string.settings_learning else R.string.settings_known),
            packs = state.packs,
            selected = if (side == PickerSide.LEARNING) state.learningLang else state.knownLang,
            onSelect = { code ->
                if (side == PickerSide.LEARNING) viewModel.setLearningLanguage(code) else viewModel.setKnownLanguage(code)
                picker = null
            },
            onDismiss = { picker = null }
        )
    }
}

@Composable
fun DictionaryContent(
    state: DictionaryUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onSwap: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onDownloadMissing: () -> Unit,
    onTranslationChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenWord: (Long) -> Unit
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    // Measured, so large text and small screens can never hide the result.
    var headerHeight by remember { mutableIntStateOf(0) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val found = state.lookup as? LookupState.Found

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .haze(hazeState)
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start = Spacing.xl,
                        end = Spacing.xl,
                        top = with(density) { headerHeight.toDp() } + Spacing.l,
                        bottom = navBottom + Spacing.navigationClearance + if (found != null) 88.dp else Spacing.l
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            if (state.missingPacks.isNotEmpty()) {
                PackNotice(state, onDownloadMissing)
            }
            when (val lookup = state.lookup) {
                LookupState.Idle -> if (state.missingPacks.isEmpty()) {
                    Text(
                        stringResource(R.string.dictionary_idle, languageName(state.fromLang)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WordMintTheme.colors.inkSecondary
                    )
                }
                LookupState.Loading -> Box(Modifier.fillMaxWidth().padding(Spacing.xxl), Alignment.Center) {
                    CircularProgressIndicator(color = WordMintTheme.colors.primary, strokeWidth = 2.dp)
                }
                LookupState.PacksRequired -> MessageBlock(
                    title = stringResource(R.string.dictionary_packs_required_title),
                    body = stringResource(R.string.dictionary_packs_required_body)
                )
                is LookupState.Found -> LookupResultContent(
                    lookup = lookup.lookup,
                    translation = state.savedTranslation ?: state.translationDraft,
                    isTranslationEditable = state.savedWordId == null,
                    onTranslationChange = onTranslationChange,
                    onRetry = onSearch
                )
            }
        }

        SearchHeader(
            state = state,
            hazeState = hazeState,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onClear = onClear,
            onSwap = onSwap,
            onPickFrom = onPickFrom,
            onPickTo = onPickTo,
            modifier = Modifier.onSizeChanged { headerHeight = it.height }
        )

        if (found != null) {
            GlassSurface(
                hazeState = hazeState,
                shape = Radii.l,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = Spacing.l, end = Spacing.l, bottom = navBottom + 88.dp)
                    .fillMaxWidth()
            ) {
                val saved = state.savedWordId
                if (saved != null) {
                    SecondaryButton(
                        text = stringResource(R.string.dictionary_in_your_words),
                        onClick = { onOpenWord(saved) },
                        icon = AppIcons.Check,
                        contentColor = WordMintTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.s)
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.dictionary_save),
                        onClick = onSave,
                        enabled = state.canSave,
                        isLoading = state.isSaving,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.s)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    state: DictionaryUiState,
    hazeState: dev.chrisbanes.haze.HazeState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onSwap: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WordMintTheme.colors
    GlassSurface(hazeState = hazeState, shape = Radii.header, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = Spacing.m).padding(bottom = Spacing.m)) {
            ScreenTitle(
                stringResource(R.string.nav_dictionary),
                Modifier.padding(start = Spacing.m, top = Spacing.m, bottom = Spacing.xs)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageChoice(
                        text = languageName(state.fromLang),
                        description = stringResource(R.string.dictionary_change_language, languageName(state.fromLang)),
                        onClick = onPickFrom,
                        modifier = Modifier.weight(1f)
                    )
                    IconAction(AppIcons.Swap, stringResource(R.string.dictionary_swap), onSwap, tint = colors.primary)
                    LanguageChoice(
                        text = languageName(state.toLang),
                        description = stringResource(R.string.dictionary_change_language, languageName(state.toLang)),
                        onClick = onPickTo,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.dictionary_placeholder),
                        onClear = onClear,
                        onSearch = onSearch,
                        modifier = Modifier.weight(1f)
                    )
                    // Stays in the row, so the header keeps its height as the user types.
                    AnimatedVisibility(state.query.isNotBlank()) {
                        QuietButton(
                            text = stringResource(R.string.dictionary_look_up),
                            onClick = onSearch,
                            enabled = state.canSearch,
                            modifier = Modifier.testTag("look_up")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChoice(text: String, description: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = WordMintTheme.colors.ink, maxLines = 1)
    }
}

@Composable
private fun PackNotice(state: DictionaryUiState, onDownload: () -> Unit) {
    val names = state.missingPacks.joinToString(", ") { languageName(it) }
    val isDownloading = state.downloadingPacks.isNotEmpty()
    Notice(
        text = stringResource(
            if (isDownloading) R.string.dictionary_packs_downloading else R.string.dictionary_packs_missing,
            names
        ),
        icon = AppIcons.Download,
        action = {
            if (!isDownloading) {
                QuietButton(stringResource(R.string.dictionary_packs_download), onDownload)
            }
        }
    )
}

@Composable
private fun LookupResultContent(
    lookup: WordLookup,
    translation: String,
    isTranslationEditable: Boolean,
    onTranslationChange: (String) -> Unit,
    onRetry: () -> Unit
) {
    val colors = WordMintTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    lookup.term,
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.ink,
                    modifier = Modifier.weight(1f).testTag("lookup_term")
                )
                PronounceButton(lookup.term, lookup.sourceLang)
            }
            lookup.phonetic?.let {
                Text(it, style = WordMintTheme.type.phonetic, color = colors.inkSecondary)
            }
        }

        TranslationField(translation, lookup.targetLang, isTranslationEditable, onTranslationChange)

        when (lookup.dictionaryStatus) {
            DictionaryStatus.FOUND -> WordEntryContent(lookup.meanings, lookup.translatedDefinitions)
            DictionaryStatus.NOT_FOUND -> Notice(stringResource(R.string.dictionary_not_found))
            DictionaryStatus.OFFLINE -> Notice(
                stringResource(R.string.dictionary_offline),
                action = { QuietButton(stringResource(R.string.action_try_again), onRetry) }
            )
            DictionaryStatus.SERVICE_ERROR -> Notice(
                stringResource(R.string.dictionary_service_error),
                action = { QuietButton(stringResource(R.string.action_try_again), onRetry) }
            )
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

/** The app's translation is a suggestion, so the user can always correct it before saving. */
@Composable
private fun TranslationField(value: String, targetLang: String, isEditable: Boolean, onChange: (String) -> Unit) {
    val colors = WordMintTheme.colors
    val label = stringResource(
        if (isEditable) R.string.dictionary_translation_label else R.string.dictionary_translation_saved,
        languageName(targetLang)
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkSecondary)
        Box {
            if (value.isEmpty()) {
                Text(
                    stringResource(R.string.dictionary_translation_placeholder),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.inkMuted
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                readOnly = !isEditable,
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = colors.primary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label }
                    .testTag("translation_field")
            )
        }
        Hairline()
    }
}
