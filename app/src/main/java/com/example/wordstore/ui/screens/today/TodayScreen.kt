package com.example.wordstore.ui.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.model.partOfSpeech
import com.example.wordstore.ui.components.GlassTube
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LabelValueRow
import com.example.wordstore.ui.components.LoadingBlock
import com.example.wordstore.ui.components.PrimaryButton
import com.example.wordstore.ui.components.SecondaryButton
import com.example.wordstore.ui.components.SectionTitle
import com.example.wordstore.ui.format.dueText
import com.example.wordstore.ui.format.todayHeading
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.TodayUiState
import com.example.wordstore.viewmodel.TodayViewModel
import com.example.wordstore.viewmodel.appViewModel

@Composable
fun TodayScreen(
    container: AppContainer,
    onStudy: () -> Unit,
    onLookUp: () -> Unit,
    onSettings: () -> Unit,
    onOpenWord: (Long) -> Unit
) {
    val viewModel = appViewModel { TodayViewModel(it.study, it.words, it.settings, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayContent(state, onStudy, onLookUp, onSettings, onOpenWord)
}

@Composable
fun TodayContent(
    state: TodayUiState,
    onStudy: () -> Unit,
    onLookUp: () -> Unit,
    onSettings: () -> Unit,
    onOpenWord: (Long) -> Unit
) {
    if (state.isLoading) {
        LoadingBlock()
        return
    }
    val colors = WordMintTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.navigationClearance + Spacing.xl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = todayHeading(state.nowMillis),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkSecondary,
                modifier = Modifier.weight(1f)
            )
            IconAction(AppIcons.Settings, stringResource(R.string.settings_title), onSettings)
        }

        Hero(state)

        Spacer(Modifier.height(Spacing.xl))

        when {
            state.hasWork -> PrimaryButton(
                text = stringResource(if (state.hasStartedToday) R.string.today_continue else R.string.today_start),
                onClick = onStudy,
                modifier = Modifier.fillMaxWidth()
            )
            else -> SecondaryButton(
                text = stringResource(R.string.today_look_up),
                onClick = onLookUp,
                icon = AppIcons.Dictionary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.totalWords > 0) {
            Plan(state, Modifier.padding(top = Spacing.xxl))
        }
        if (state.recentWords.isNotEmpty()) {
            RecentWords(state.recentWords, onOpenWord, Modifier.padding(top = Spacing.xxl))
        }
    }
}

@Composable
private fun Hero(state: TodayUiState) {
    val colors = WordMintTheme.colors
    val headline = when {
        state.totalWords == 0 -> stringResource(R.string.today_empty_title)
        state.hasWork && state.hasStartedToday ->
            pluralStringResource(R.plurals.today_left, state.waiting, state.waiting)
        state.hasWork -> pluralStringResource(R.plurals.today_waiting, state.waiting, state.waiting)
        state.hasStartedToday -> stringResource(R.string.today_done_title)
        else -> stringResource(R.string.today_clear_title)
    }
    val support = when {
        state.totalWords == 0 -> stringResource(R.string.today_empty_body)
        state.hasStartedToday ->
            pluralStringResource(R.plurals.today_reviewed_count, state.reviewsToday, state.reviewsToday)
        state.hasWork -> stringResource(R.string.today_saved_as_you_go)
        else -> null
    }
    val next = state.nextDueAt?.takeIf { !state.hasWork }
        ?.let { stringResource(R.string.today_next_review, dueText(it, state.nowMillis)) }

    val tubeDescription = pluralStringResource(
        R.plurals.today_tube_description,
        state.todayTotal,
        state.reviewsToday,
        state.todayTotal
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.displayMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
            if (support != null) {
                Text(support, style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary)
            }
            if (next != null) {
                Text(next, style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary)
            }
        }
        GlassTube(
            level = state.level,
            graduations = graduationsFor(state.todayTotal),
            modifier = Modifier
                .size(width = 70.dp, height = 204.dp)
                .semantics { contentDescription = tubeDescription }
        )
    }
}

/** One tick per word for a short day. Longer days use a fixed scale, so ticks stay readable. */
private fun graduationsFor(total: Int): Int = when {
    total == 0 -> 5
    total <= 20 -> total
    else -> 20
}

@Composable
private fun Plan(state: TodayUiState, modifier: Modifier = Modifier) {
    val colors = WordMintTheme.colors
    Column(modifier) {
        SectionTitle(stringResource(R.string.today_plan))
        Spacer(Modifier.height(Spacing.s))
        LabelValueRow(
            stringResource(R.string.today_plan_reviews),
            state.dueReviews.toString(),
            valueColor = if (state.dueReviews > 0) colors.copper else colors.ink
        )
        Hairline()
        LabelValueRow(stringResource(R.string.today_plan_steps), state.inSteps.toString())
        Hairline()
        LabelValueRow(
            stringResource(R.string.today_plan_new),
            stringResource(R.string.today_plan_new_value, state.newStartedToday, state.dailyNewGoal)
        )
        Hairline()
        LabelValueRow(
            stringResource(R.string.today_streak),
            pluralStringResource(R.plurals.days_count, state.currentStreak, state.currentStreak)
        )
    }
}

@Composable
private fun RecentWords(words: List<Word>, onOpenWord: (Long) -> Unit, modifier: Modifier = Modifier) {
    val colors = WordMintTheme.colors
    Column(modifier) {
        SectionTitle(stringResource(R.string.today_recent))
        Spacer(Modifier.height(Spacing.s))
        words.forEachIndexed { index, word ->
            if (index > 0) Hairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenWord(word.id) }
                    .heightIn(min = 56.dp)
                    .padding(vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Text(
                    word.term,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                word.partOfSpeech?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = WordMintTheme.type.partOfSpeech, color = colors.inkMuted, maxLines = 1)
                }
                Text(
                    word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
