package com.example.wordstore.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.data.local.database.HistoryRow
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.ProgressCalculator
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.LabelValueRow
import com.example.wordstore.ui.components.LoadingBlock
import com.example.wordstore.ui.components.MessageBlock
import com.example.wordstore.ui.components.ScreenTitle
import com.example.wordstore.ui.components.SectionTitle
import com.example.wordstore.ui.components.TextTab
import com.example.wordstore.ui.format.dateTimeText
import com.example.wordstore.ui.format.dayText
import com.example.wordstore.ui.format.percentText
import com.example.wordstore.ui.format.ratingLabel
import com.example.wordstore.ui.format.shortInterval
import com.example.wordstore.ui.format.weekdayInitial
import com.example.wordstore.ui.screens.library.statusColor
import com.example.wordstore.ui.screens.library.statusLabel
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.ActivityRange
import com.example.wordstore.viewmodel.ProgressUiState
import com.example.wordstore.viewmodel.ProgressViewModel
import com.example.wordstore.viewmodel.appViewModel
import java.time.LocalDate
import kotlin.math.ceil

@Composable
fun ProgressScreen(container: AppContainer, onOpenWord: (Long) -> Unit) {
    val viewModel = appViewModel { ProgressViewModel(it.study, it.words, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressContent(state, viewModel::setRange, onOpenWord)
}

@Composable
fun ProgressContent(
    state: ProgressUiState,
    onRangeChange: (ActivityRange) -> Unit,
    onOpenWord: (Long) -> Unit
) {
    if (state.isLoading) {
        LoadingBlock()
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(top = Spacing.m, bottom = Spacing.navigationClearance + Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
    ) {
        ScreenTitle(stringResource(R.string.nav_progress))
        if (!state.hasReviews) {
            MessageBlock(
                title = stringResource(R.string.progress_empty_title),
                body = stringResource(R.string.progress_empty_body)
            )
        } else {
            ActivitySection(state, onRangeChange)
            RecallSection(state)
            ConsistencySection(state)
        }
        if (state.totalWords > 0) {
            VocabularySection(state)
        }
        if (state.forecast.any { it.second > 0 }) {
            ForecastSection(state.forecast)
        }
        if (state.hasReviews) {
            SummarySection(state)
            HistorySection(state.history, onOpenWord)
        }
    }
}

@Composable
private fun ActivitySection(state: ProgressUiState, onRangeChange: (ActivityRange) -> Unit) {
    val colors = WordMintTheme.colors
    var selectedDay by remember(state.range) { mutableStateOf<Long?>(null) }
    val days = state.activity
    val selected = days.firstOrNull { it.day == selectedDay } ?: days.lastOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.progress_activity), Modifier.weight(1f))
            TextTab(stringResource(R.string.progress_range_week), state.range == ActivityRange.WEEK, { onRangeChange(ActivityRange.WEEK) })
            TextTab(stringResource(R.string.progress_range_month), state.range == ActivityRange.MONTH, { onRangeChange(ActivityRange.MONTH) })
        }
        if (selected != null) {
            Text(
                stringResource(
                    R.string.progress_day_detail,
                    dayText(selected.day),
                    pluralStringResource(R.plurals.reviews_count, selected.reviews, selected.reviews),
                    pluralStringResource(R.plurals.new_words_count, selected.newStarted, selected.newStarted)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )
        }
        BarChart(
            values = days.map { it.reviews },
            emphasized = days.map { it.goalMet },
            selectedIndex = days.indexOfFirst { it.day == selected?.day },
            description = stringResource(
                R.string.progress_activity_description,
                pluralStringResource(R.plurals.reviews_count, days.sumOf { it.reviews }, days.sumOf { it.reviews }),
                days.count { it.reviews > 0 },
                days.size
            ),
            onSelect = { selectedDay = days[it].day }
        )
        AxisLabels(days.map { it.day })
        Text(
            stringResource(R.string.progress_goal_legend),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted
        )
    }
}

/** Thin bars on a shared baseline, one series, on one axis. */
@Composable
private fun BarChart(
    values: List<Int>,
    emphasized: List<Boolean>,
    selectedIndex: Int,
    description: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WordMintTheme.colors
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(112.dp)
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val index = (offset.x / size.width * values.size).toInt().coerceIn(0, values.lastIndex)
                    onSelect(index)
                }
            }
            .semantics { contentDescription = description }
    ) {
        val slot = size.width / values.size
        val barWidth = (slot * 0.62f).coerceAtMost(18.dp.toPx())
        val baseline = size.height - 1.dp.toPx()
        val radius = 4.dp.toPx().coerceAtMost(barWidth / 2)

        drawLine(colors.hairline, Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())

        values.forEachIndexed { index, value ->
            if (value == 0) return@forEachIndexed
            val height = (value.toFloat() / max * (baseline - 8.dp.toPx())).coerceAtLeast(3.dp.toPx())
            val left = slot * index + (slot - barWidth) / 2
            val rect = Rect(left, baseline - height, left + barWidth, baseline)
            val alpha = if (emphasized.getOrElse(index) { false }) 1f else 0.45f
            drawPath(
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect,
                            topLeft = CornerRadius(radius),
                            topRight = CornerRadius(radius),
                            bottomLeft = CornerRadius.Zero,
                            bottomRight = CornerRadius.Zero
                        )
                    )
                },
                colors.primary.copy(alpha = alpha)
            )
        }

        // Selection is a mark on the baseline, because bar shade already carries meaning.
        if (selectedIndex in values.indices) {
            val center = slot * selectedIndex + slot / 2
            drawLine(
                colors.ink,
                Offset(center - barWidth / 2, baseline),
                Offset(center + barWidth / 2, baseline),
                2.dp.toPx()
            )
        }
    }
}

@Composable
private fun AxisLabels(days: List<Long>) {
    val colors = WordMintTheme.colors
    Row(Modifier.fillMaxWidth()) {
        if (days.size <= 7) {
            days.forEach { day ->
                Text(
                    weekdayInitial(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Text(dayText(days.first()), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
            Spacer(Modifier.weight(1f))
            Text(dayText(days.last()), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        }
    }
}

@Composable
private fun RecallSection(state: ProgressUiState) {
    val colors = WordMintTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(stringResource(R.string.progress_recall))
        val rate = state.recallRate
        if (rate == null) {
            Text(
                pluralStringResource(
                    R.plurals.progress_recall_waiting,
                    state.recall.total,
                    ProgressCalculator.MIN_REVIEWS_FOR_RECALL,
                    state.recall.total
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary
            )
        } else {
            Text(percentText(rate), style = WordMintTheme.type.figureLarge, color = colors.ink)
            Text(
                pluralStringResource(R.plurals.progress_recall_body, state.recall.total, state.recall.total),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary
            )
        }
    }
}

@Composable
private fun ConsistencySection(state: ProgressUiState) {
    val colors = WordMintTheme.colors
    val days = state.consistency
    val active = days.count { it.reviews > 0 }
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionTitle(stringResource(R.string.progress_consistency))
        val selected = selectedDay
        Text(
            if (selected != null) {
                stringResource(
                    R.string.progress_consistency_day,
                    dayText(selected.day),
                    pluralStringResource(R.plurals.reviews_count, selected.reviews, selected.reviews)
                )
            } else {
                stringResource(R.string.progress_consistency_summary, active, days.size)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkSecondary
        )
        ConsistencyGrid(days, stringResource(R.string.progress_consistency_summary, active, days.size)) { selectedDay = it }
    }
}

/** Weeks across, days down, darker for more reviews. A single hue, because this is magnitude. */
@Composable
private fun ConsistencyGrid(days: List<DayActivity>, description: String, onSelect: (DayActivity) -> Unit) {
    val colors = WordMintTheme.colors
    val max = (days.maxOfOrNull { it.reviews } ?: 0).coerceAtLeast(1)
    val rows = 7
    // Each row is one weekday, Monday on top, so patterns in the week can be seen.
    val lead = days.firstOrNull()?.let { LocalDate.ofEpochDay(it.day).dayOfWeek.value - 1 } ?: 0
    val columns = ceil((days.size + lead) / rows.toFloat()).toInt().coerceAtLeast(1)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(132.dp)
            .pointerInput(days) {
                detectTapGestures { offset ->
                    val column = (offset.x / size.width * columns).toInt()
                    val row = (offset.y / size.height * rows).toInt()
                    days.getOrNull(column * rows + row - lead)?.let(onSelect)
                }
            }
            .semantics { contentDescription = description }
    ) {
        val gap = 2.dp.toPx()
        val cellWidth = (size.width - gap * (columns - 1)) / columns
        val cellHeight = (size.height - gap * (rows - 1)) / rows
        days.forEachIndexed { index, day ->
            val column = (index + lead) / rows
            val row = (index + lead) % rows
            val color = if (day.reviews == 0) {
                colors.surfaceSunken
            } else {
                colors.primary.copy(alpha = 0.3f + 0.7f * day.reviews / max)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(column * (cellWidth + gap), row * (cellHeight + gap)),
                size = Size(cellWidth, cellHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
private fun VocabularySection(state: ProgressUiState) {
    val colors = WordMintTheme.colors
    val order = listOf(LearningStatus.NEW, LearningStatus.LEARNING, LearningStatus.MASTERED)
    val counts = order.map { it to (state.statusCounts[it] ?: 0) }
    val total = counts.sumOf { it.second }.coerceAtLeast(1)
    val segments = counts.filter { it.second > 0 }.map { (status, count) -> statusColor(status) to count }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionTitle(pluralStringResource(R.plurals.progress_vocabulary, state.totalWords, state.totalWords))
        val summary = stringResource(
            R.string.progress_distribution_description,
            counts[0].second,
            counts[1].second,
            counts[2].second
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .semantics { contentDescription = summary }
        ) {
            val gap = 2.dp.toPx()
            val usable = size.width - gap * (segments.size - 1).coerceAtLeast(0)
            var x = 0f
            val radius = CornerRadius(4.dp.toPx())
            segments.forEach { (color, count) ->
                val width = usable * count / total
                drawRoundRect(color, Offset(x, 0f), Size(width, size.height), radius)
                x += width + gap
            }
        }
        counts.forEach { (status, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Box(Modifier.size(10.dp).background(statusColor(status), CircleShape))
                Text(statusLabel(status), style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary, modifier = Modifier.weight(1f))
                Text(count.toString(), style = MaterialTheme.typography.titleSmall, color = colors.ink)
            }
        }
    }
}

@Composable
private fun ForecastSection(forecast: List<Pair<Long, Int>>) {
    val colors = WordMintTheme.colors
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selected = forecast.getOrNull(selectedIndex)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionTitle(stringResource(R.string.progress_forecast))
        if (selected != null) {
            Text(
                stringResource(
                    R.string.progress_consistency_day,
                    dayText(selected.first),
                    pluralStringResource(R.plurals.reviews_count, selected.second, selected.second)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )
        }
        BarChart(
            values = forecast.map { it.second },
            emphasized = forecast.map { true },
            selectedIndex = selectedIndex,
            description = stringResource(
                R.string.progress_forecast_description,
                pluralStringResource(R.plurals.reviews_count, forecast.sumOf { it.second }, forecast.sumOf { it.second })
            ),
            onSelect = { selectedIndex = it }
        )
        Row(Modifier.fillMaxWidth()) {
            forecast.forEach { (day, _) ->
                Text(
                    weekdayInitial(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SummarySection(state: ProgressUiState) {
    Column {
        SectionTitle(stringResource(R.string.progress_totals))
        Spacer(Modifier.height(Spacing.s))
        LabelValueRow(stringResource(R.string.progress_total_reviews), state.totalReviews.toString())
        Hairline()
        LabelValueRow(
            stringResource(R.string.today_streak),
            pluralStringResource(R.plurals.days_count, state.currentStreak, state.currentStreak)
        )
        Hairline()
        LabelValueRow(
            stringResource(R.string.progress_best_streak),
            pluralStringResource(R.plurals.days_count, state.bestStreak, state.bestStreak)
        )
        Hairline()
        LabelValueRow(
            stringResource(R.string.progress_goals_met),
            stringResource(R.string.progress_goals_met_value, state.goalsMet, ProgressViewModel.RECALL_WINDOW_DAYS)
        )
    }
}

@Composable
private fun HistorySection(history: List<HistoryRow>, onOpenWord: (Long) -> Unit) {
    val colors = WordMintTheme.colors
    Column {
        SectionTitle(stringResource(R.string.progress_history))
        Spacer(Modifier.height(Spacing.s))
        history.forEachIndexed { index, row ->
            if (index > 0) Hairline()
            val wordId = row.wordId
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (wordId != null) Modifier.clickable { onOpenWord(wordId) } else Modifier)
                    .heightIn(min = 56.dp)
                    .padding(vertical = Spacing.s)
                    .testTag("history_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.term ?: stringResource(R.string.progress_deleted_word),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (row.term != null) colors.ink else colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(dateTimeText(row.reviewedAt), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
                Text(
                    stringResource(
                        R.string.detail_history_entry,
                        ratingLabel(Rating.fromValue(row.rating)),
                        shortInterval(row.intervalMillis)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )
            }
        }
    }
}
