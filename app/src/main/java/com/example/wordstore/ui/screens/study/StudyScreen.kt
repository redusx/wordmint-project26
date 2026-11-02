package com.example.wordstore.ui.screens.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.model.partOfSpeech
import com.example.wordstore.domain.repository.StudyCard
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.ui.components.GlassSurface
import com.example.wordstore.ui.components.GlassTube
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LabelValueRow
import com.example.wordstore.ui.components.LoadingBlock
import com.example.wordstore.ui.components.MessageBlock
import com.example.wordstore.ui.components.PrimaryButton
import com.example.wordstore.ui.components.PronounceButton
import com.example.wordstore.ui.components.QuietButton
import com.example.wordstore.ui.components.SecondaryButton
import com.example.wordstore.ui.components.TubeOrientation
import com.example.wordstore.ui.components.WordEntryContent
import com.example.wordstore.ui.format.dueText
import com.example.wordstore.ui.format.durationText
import com.example.wordstore.ui.format.ratingLabel
import com.example.wordstore.ui.format.shortInterval
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Motion
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.RatingFeedback
import com.example.wordstore.viewmodel.RatingOption
import com.example.wordstore.viewmodel.SessionSummary
import com.example.wordstore.viewmodel.StudyPhase
import com.example.wordstore.viewmodel.StudyUiState
import com.example.wordstore.viewmodel.StudyViewModel
import com.example.wordstore.viewmodel.appViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun StudyScreen(container: AppContainer, onExit: () -> Unit, onLookUp: () -> Unit) {
    val viewModel = appViewModel { StudyViewModel(it.study, it.scheduler, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    StudyContent(
        state = state,
        nowMillis = container.clock.millis(),
        onShowHint = viewModel::showHint,
        onReveal = viewModel::reveal,
        onRate = viewModel::rate,
        onRetry = viewModel::retry,
        onExit = onExit,
        onLookUp = onLookUp
    )
}

@Composable
fun StudyContent(
    state: StudyUiState,
    nowMillis: Long,
    onShowHint: () -> Unit,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    onLookUp: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        when (val phase = state.phase) {
            StudyPhase.Loading -> LoadingBlock()
            is StudyPhase.Empty -> Finished {
                MessageBlock(
                    title = stringResource(R.string.study_empty_title),
                    body = phase.nextDueAt
                        ?.let { stringResource(R.string.today_next_review, dueText(it, nowMillis)) }
                        ?: stringResource(R.string.study_empty_body),
                    action = {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            PrimaryButton(stringResource(R.string.today_look_up), onLookUp, Modifier.fillMaxWidth())
                            SecondaryButton(stringResource(R.string.study_back_today), onExit, Modifier.fillMaxWidth())
                        }
                    }
                )
            }
            is StudyPhase.Failed -> Finished {
                MessageBlock(
                    title = stringResource(R.string.study_failed_title),
                    body = stringResource(R.string.study_failed_body),
                    action = { PrimaryButton(stringResource(R.string.action_try_again), onRetry, Modifier.fillMaxWidth()) }
                )
            }
            is StudyPhase.Complete -> SessionComplete(phase.summary, nowMillis, onExit)
            is StudyPhase.Studying -> Session(state, phase, onShowHint, onReveal, onRate, onExit)
        }
    }
}

@Composable
private fun Finished(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().navigationBarsPadding().padding(Spacing.xl),
        contentAlignment = Alignment.BottomStart
    ) { content() }
}

@Composable
private fun Session(
    state: StudyUiState,
    phase: StudyPhase.Studying,
    onShowHint: () -> Unit,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    onExit: () -> Unit
) {
    val hazeState = remember { HazeState() }

    SessionHeader(state, onExit)

    Box(Modifier.fillMaxSize().padding(horizontal = Spacing.m).navigationBarsPadding()) {
        // Keyed on the repetition count, so a card seen again still transitions, while revealing
        // the answer stays inside the same card.
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (slideInHorizontally(tween(Motion.STANDARD, easing = Motion.Emphasized)) { it / 2 } + fadeIn(tween(Motion.STANDARD)))
                    .togetherWith(
                        slideOutHorizontally(tween(Motion.STANDARD, easing = Motion.Emphasized)) { -it / 3 } +
                            fadeOut(tween(Motion.QUICK)) +
                            scaleOut(tween(Motion.STANDARD), targetScale = 0.96f)
                    )
            },
            contentKey = { it.card.word.id to it.card.memory.reps },
            label = "card_transition"
        ) { shown ->
            Flashcard(shown, hazeState, onShowHint)
        }

        StudyDock(
            phase = phase,
            hazeState = hazeState,
            onReveal = onReveal,
            onRate = onRate,
            modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.s)
        )
    }
}

@Composable
private fun SessionHeader(state: StudyUiState, onExit: () -> Unit) {
    val colors = WordMintTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(AppIcons.Close, stringResource(R.string.study_exit), onExit)
            GlassTube(
                level = state.level,
                orientation = TubeOrientation.HORIZONTAL,
                modifier = Modifier.weight(1f).height(12.dp)
            )
            Text(
                text = stringResource(R.string.study_position, state.position, state.total),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkSecondary,
                modifier = Modifier.padding(horizontal = Spacing.m)
            )
        }
        Box(Modifier.fillMaxWidth().height(24.dp).padding(start = Spacing.m), contentAlignment = Alignment.CenterStart) {
            androidx.compose.animation.AnimatedVisibility(
                visible = state.feedback != null,
                enter = fadeIn(tween(Motion.STANDARD)),
                exit = fadeOut(tween(Motion.QUICK))
            ) {
                state.feedback?.let { FeedbackLine(it) }
            }
        }
    }
}

@Composable
private fun FeedbackLine(feedback: RatingFeedback) {
    Text(
        text = stringResource(R.string.study_feedback, ratingLabel(feedback.rating), shortInterval(feedback.intervalMillis)),
        style = MaterialTheme.typography.bodySmall,
        color = WordMintTheme.colors.inkSecondary
    )
}

@Composable
private fun Flashcard(phase: StudyPhase.Studying, hazeState: HazeState, onShowHint: () -> Unit) {
    val colors = WordMintTheme.colors
    val card = phase.card
    val word = card.word
    val reveal by animateFloatAsState(
        targetValue = if (phase.isRevealed) 1f else 0f,
        animationSpec = tween(Motion.STANDARD, easing = Motion.Emphasized),
        label = "reveal"
    )
    val topSpace by animateDpAsState(
        targetValue = if (phase.isRevealed) Spacing.xl else 104.dp,
        animationSpec = tween(Motion.STANDARD, easing = Motion.Emphasized),
        label = "reveal_top"
    )
    val typography = MaterialTheme.typography
    val termStyle: TextStyle = lerp(typography.displayLarge, typography.headlineMedium, reveal)

    Box(
        Modifier
            .fillMaxSize()
            .clip(Radii.xl)
            .background(colors.surface)
            .haze(hazeState)
            .testTag("flashcard")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
                .padding(bottom = 132.dp)
        ) {
            Spacer(Modifier.height(topSpace))

            if (card.memory.state == CardState.NEW && !phase.isRevealed) {
                Text(
                    stringResource(R.string.study_new_word),
                    style = typography.labelMedium,
                    color = colors.copper,
                    modifier = Modifier.padding(bottom = Spacing.s)
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                Text(word.term, style = termStyle, color = colors.ink, modifier = Modifier.weight(1f))
                PronounceButton(word.term, word.sourceLang, audioUrl = word.audioUrl)
            }

            word.phonetic?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    style = WordMintTheme.type.phonetic,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            AnimatedVisibility(
                visible = !phase.isRevealed,
                exit = fadeOut(tween(Motion.QUICK))
            ) {
                Hint(phase, onShowHint)
            }

            AnimatedVisibility(
                visible = phase.isRevealed,
                enter = fadeIn(tween(Motion.STANDARD)) + expandVertically(tween(Motion.STANDARD, easing = Motion.Emphasized), expandFrom = Alignment.Top),
                exit = fadeOut(tween(Motion.QUICK))
            ) {
                Column(Modifier.padding(top = Spacing.xl)) {
                    Text(
                        word.translation,
                        style = typography.displayMedium,
                        color = colors.primary,
                        modifier = Modifier.testTag("translation")
                    )
                    word.partOfSpeech?.takeIf { word.meanings.isEmpty() && it.isNotBlank() }?.let {
                        Text(it, style = WordMintTheme.type.partOfSpeech, color = colors.inkSecondary)
                    }
                    word.hint?.takeIf(String::isNotBlank)?.let {
                        Text(
                            it,
                            style = typography.bodyLarge,
                            color = colors.inkSecondary,
                            modifier = Modifier.padding(top = Spacing.s)
                        )
                    }
                    if (word.meanings.isNotEmpty() || word.translatedDefinitions.isNotEmpty()) {
                        Hairline(Modifier.padding(vertical = Spacing.xl))
                        WordEntryContent(word.meanings, word.translatedDefinitions)
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(phase: StudyPhase.Studying, onShowHint: () -> Unit) {
    val colors = WordMintTheme.colors
    val hint = phase.hint ?: return
    Column(Modifier.padding(top = Spacing.xl)) {
        if (phase.isHintShown) {
            Text(hint, style = WordMintTheme.type.example, color = colors.inkSecondary)
        } else {
            QuietButton(
                text = stringResource(R.string.study_show_hint),
                onClick = onShowHint,
                icon = AppIcons.Hint,
                contentColor = colors.inkSecondary,
                modifier = Modifier.offset(x = -Spacing.m)
            )
        }
    }
}

@Composable
private fun StudyDock(
    phase: StudyPhase.Studying,
    hazeState: HazeState,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(hazeState = hazeState, shape = Radii.l, modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = phase.isRevealed,
            transitionSpec = { fadeIn(tween(Motion.STANDARD)) togetherWith fadeOut(tween(Motion.QUICK)) },
            label = "dock"
        ) { revealed ->
            if (revealed) {
                RatingRow(phase.options, enabled = !phase.isSaving, onRate = onRate)
            } else {
                PrimaryButton(
                    text = stringResource(R.string.study_show_answer),
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.s)
                )
            }
        }
    }
}

@Composable
private fun RatingRow(options: List<RatingOption>, enabled: Boolean, onRate: (Rating) -> Unit) {
    val colors = WordMintTheme.colors
    Row(Modifier.fillMaxWidth().padding(Spacing.xs)) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(Sizes.ratingButton - 28.dp)
                        .align(Alignment.CenterVertically)
                        .background(colors.hairline)
                )
            }
            RatingButton(option, enabled, onRate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RatingButton(option: RatingOption, enabled: Boolean, onRate: (Rating) -> Unit, modifier: Modifier) {
    val colors = WordMintTheme.colors
    val intervalColor: Color = when (option.rating) {
        Rating.AGAIN -> colors.danger
        Rating.HARD -> colors.bronze
        Rating.GOOD -> colors.verdigris
        Rating.EASY -> colors.sea
    }
    val label = ratingLabel(option.rating)
    val interval = shortInterval(option.intervalMillis)
    val description = stringResource(R.string.study_rating_description, label, interval)
    Column(
        modifier = modifier
            .heightIn(min = Sizes.ratingButton)
            .clip(Radii.m)
            .clickable(enabled = enabled) { onRate(option.rating) }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .testTag("rate_${option.rating.name}")
            .padding(vertical = Spacing.m, horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.ink, maxLines = 1)
        Text(interval, style = MaterialTheme.typography.bodySmall, color = intervalColor, maxLines = 1)
    }
}

@Composable
private fun SessionComplete(summary: SessionSummary, nowMillis: Long, onDone: () -> Unit) {
    val colors = WordMintTheme.colors
    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l)
    ) {
        Spacer(Modifier.height(Spacing.xxl))
        Text(
            pluralStringResource(R.plurals.study_complete_title, summary.reviewed, summary.reviewed),
            style = MaterialTheme.typography.displayMedium,
            color = colors.ink
        )
        Column {
            LabelValueRow(stringResource(R.string.study_complete_time), durationText(summary.activeMillis))
            Hairline()
            LabelValueRow(stringResource(R.string.study_complete_new), summary.newStarted.toString())
            summary.nextDueAt?.let {
                Hairline()
                LabelValueRow(stringResource(R.string.study_complete_next), dueText(it, nowMillis))
            }
        }
        Column {
            Text(
                stringResource(R.string.study_complete_ratings),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
            Text(
                stringResource(R.string.study_complete_ratings_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.s)
            )
            Rating.entries.forEachIndexed { index, rating ->
                if (index > 0) Hairline()
                LabelValueRow(ratingLabel(rating), (summary.ratings[rating] ?: 0).toString())
            }
        }
        Spacer(Modifier.height(Spacing.s))
        PrimaryButton(stringResource(R.string.study_back_today), onDone, Modifier.fillMaxWidth())
    }
}
