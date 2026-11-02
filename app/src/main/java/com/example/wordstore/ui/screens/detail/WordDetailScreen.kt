package com.example.wordstore.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.ui.components.GlassTube
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LabelValueRow
import com.example.wordstore.ui.components.LoadingBlock
import com.example.wordstore.ui.components.MessageBlock
import com.example.wordstore.ui.components.PrimaryButton
import com.example.wordstore.ui.components.PronounceButton
import com.example.wordstore.ui.components.QuietButton
import com.example.wordstore.ui.components.SectionTitle
import com.example.wordstore.ui.components.TubeOrientation
import com.example.wordstore.ui.components.WordEntryContent
import com.example.wordstore.ui.format.dateTimeText
import com.example.wordstore.ui.format.dueText
import com.example.wordstore.ui.format.languageName
import com.example.wordstore.ui.format.percentText
import com.example.wordstore.ui.format.ratingLabel
import com.example.wordstore.ui.format.shortInterval
import com.example.wordstore.ui.screens.library.statusColor
import com.example.wordstore.ui.screens.library.statusLabel
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.EditResult
import com.example.wordstore.viewmodel.WordDetail
import com.example.wordstore.viewmodel.WordDetailViewModel
import com.example.wordstore.viewmodel.WordEdits
import com.example.wordstore.viewmodel.appViewModel
import kotlin.math.roundToInt

@Composable
fun WordDetailScreen(container: AppContainer, wordId: Long, onBack: () -> Unit) {
    val viewModel = appViewModel(key = "word_$wordId") {
        WordDetailViewModel(wordId, it.words, it.scheduler, it.clock)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editResult by viewModel.editResult.collectAsStateWithLifecycle()
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editResult) {
        if (editResult == EditResult.SAVED) {
            isEditing = false
            viewModel.clearEditResult()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconAction(AppIcons.Back, stringResource(R.string.action_back), onBack)
            Spacer(Modifier.weight(1f))
            if (state.detail != null) {
                IconAction(AppIcons.Edit, stringResource(R.string.action_edit), { isEditing = true })
                IconAction(
                    AppIcons.Delete,
                    stringResource(R.string.action_delete),
                    { isConfirmingDelete = true },
                    tint = WordMintTheme.colors.danger
                )
            }
        }

        val detail = state.detail
        when {
            state.isLoading -> LoadingBlock()
            detail == null -> MessageBlock(
                title = stringResource(R.string.detail_gone_title),
                body = stringResource(R.string.detail_gone_body),
                modifier = Modifier.padding(Spacing.xl),
                action = { PrimaryButton(stringResource(R.string.action_back), onBack) }
            )
            else -> DetailContent(detail, state.nowMillis)
        }
    }

    val detail = state.detail
    if (isEditing && detail != null) {
        EditWordSheet(
            detail = detail,
            isDuplicate = editResult == EditResult.DUPLICATE,
            onSave = viewModel::save,
            onChanged = viewModel::clearEditResult,
            onDismiss = {
                isEditing = false
                viewModel.clearEditResult()
            }
        )
    }

    if (isConfirmingDelete && detail != null) {
        val colors = WordMintTheme.colors
        AlertDialog(
            onDismissRequest = { isConfirmingDelete = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.detail_delete_title, detail.word.term), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.detail_delete_body), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                QuietButton(
                    text = stringResource(R.string.action_delete),
                    contentColor = colors.danger,
                    onClick = {
                        viewModel.delete()
                        isConfirmingDelete = false
                        onBack()
                    }
                )
            },
            dismissButton = {
                QuietButton(stringResource(R.string.action_cancel), { isConfirmingDelete = false }, contentColor = colors.inkSecondary)
            }
        )
    }
}

@Composable
private fun DetailContent(detail: WordDetail, nowMillis: Long) {
    val colors = WordMintTheme.colors
    val word = detail.word
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    word.term,
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.ink,
                    modifier = Modifier.weight(1f).testTag("detail_term")
                )
                PronounceButton(word.term, word.sourceLang, audioUrl = word.audioUrl)
            }
            word.phonetic?.takeIf(String::isNotBlank)?.let {
                Text(it, style = WordMintTheme.type.phonetic, color = colors.inkSecondary)
            }
            Text(
                word.translation,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary,
                modifier = Modifier.padding(top = Spacing.m)
            )
            word.hint?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.inkSecondary, modifier = Modifier.padding(top = Spacing.s))
            }
        }

        LearningSection(detail, nowMillis)

        if (word.meanings.isNotEmpty() || word.translatedDefinitions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                SectionTitle(stringResource(R.string.detail_meanings))
                WordEntryContent(word.meanings, word.translatedDefinitions)
            }
        }

        Column {
            SectionTitle(stringResource(R.string.detail_history))
            Spacer(Modifier.height(Spacing.s))
            if (detail.history.isEmpty()) {
                Text(stringResource(R.string.detail_history_empty), style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary)
            }
            detail.history.forEachIndexed { index, log ->
                if (index > 0) Hairline()
                LabelValueRow(
                    label = dateTimeText(log.reviewedAt),
                    value = stringResource(
                        R.string.detail_history_entry,
                        ratingLabel(Rating.fromValue(log.rating)),
                        shortInterval(log.intervalMillis)
                    )
                )
            }
        }
    }
}

@Composable
private fun LearningSection(detail: WordDetail, nowMillis: Long) {
    val colors = WordMintTheme.colors
    val memory = detail.memory
    Column {
        SectionTitle(stringResource(R.string.detail_learning))
        Spacer(Modifier.height(Spacing.s))
        LabelValueRow(
            stringResource(R.string.detail_status),
            statusLabel(detail.status)
        )
        Hairline()
        LabelValueRow(
            stringResource(R.string.detail_next_review),
            if (detail.status == LearningStatus.NEW) {
                stringResource(R.string.detail_next_review_new)
            } else {
                dueText(memory.dueAt, nowMillis)
            },
            valueColor = if (detail.isDue) colors.copper else colors.ink
        )
        if (memory.state != CardState.NEW) {
            Hairline()
            Column(Modifier.padding(vertical = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row {
                    Text(
                        stringResource(R.string.detail_recall),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(percentText(detail.recall.toDouble()), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                }
                GlassTube(
                    level = detail.recall,
                    orientation = TubeOrientation.HORIZONTAL,
                    liquid = statusColor(detail.status),
                    liquidDeep = statusColor(detail.status),
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
            }
            memory.stability?.let { stability ->
                Hairline()
                val days = stability.roundToInt().coerceAtLeast(1)
                LabelValueRow(
                    stringResource(R.string.detail_memory),
                    pluralStringResource(R.plurals.detail_memory_days, days, days)
                )
            }
            memory.difficulty?.let { difficulty ->
                Hairline()
                LabelValueRow(
                    stringResource(R.string.detail_difficulty),
                    stringResource(R.string.detail_difficulty_value, difficulty.roundToInt())
                )
            }
            Hairline()
            LabelValueRow(stringResource(R.string.detail_reviews), memory.reps.toString())
            Hairline()
            LabelValueRow(stringResource(R.string.detail_lapses), memory.lapses.toString())
            memory.lastReviewAt?.let {
                Hairline()
                LabelValueRow(stringResource(R.string.detail_last_review), dateTimeText(it))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWordSheet(
    detail: WordDetail,
    isDuplicate: Boolean,
    onSave: (WordEdits) -> Unit,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = WordMintTheme.colors
    val word = detail.word
    var term by rememberSaveable { mutableStateOf(word.term) }
    var translation by rememberSaveable { mutableStateOf(word.translation) }
    var phonetic by rememberSaveable { mutableStateOf(word.phonetic.orEmpty()) }
    var hint by rememberSaveable { mutableStateOf(word.hint.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        shape = Radii.sheet
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            SectionTitle(stringResource(R.string.detail_edit_title))
            EditField(
                value = term,
                onChange = {
                    term = it
                    onChanged()
                },
                label = stringResource(R.string.detail_edit_term, languageName(word.sourceLang)),
                error = if (isDuplicate) stringResource(R.string.detail_edit_duplicate) else null,
                tag = "edit_term"
            )
            EditField(
                value = translation,
                onChange = { translation = it },
                label = stringResource(R.string.detail_edit_translation, languageName(word.targetLang)),
                tag = "edit_translation"
            )
            EditField(phonetic, { phonetic = it }, stringResource(R.string.detail_edit_phonetic), tag = "edit_phonetic")
            EditField(hint, { hint = it }, stringResource(R.string.detail_edit_hint), tag = "edit_hint")
            PrimaryButton(
                text = stringResource(R.string.action_save_changes),
                onClick = { onSave(WordEdits(term, translation, phonetic, hint)) },
                enabled = term.isNotBlank() && translation.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)
            )
        }
    }
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, label: String, tag: String, error: String? = null) {
    val colors = WordMintTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.titleMedium,
        shape = Radii.s,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.inkMuted.copy(alpha = 0.5f),
            focusedLabelColor = colors.primary,
            cursorColor = colors.primary
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}
