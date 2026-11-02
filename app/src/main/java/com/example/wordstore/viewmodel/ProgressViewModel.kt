package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.local.database.HistoryRow
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.ProgressCalculator
import com.example.wordstore.domain.progress.RecallCounts
import com.example.wordstore.domain.repository.StudyRepository
import com.example.wordstore.domain.repository.WordRepository
import com.example.wordstore.domain.study.LearningDay
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.domain.study.status
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ActivityRange(val days: Int) { WEEK(7), MONTH(30) }

data class ProgressUiState(
    val isLoading: Boolean = true,
    val today: Long = 0,
    val range: ActivityRange = ActivityRange.WEEK,
    val activity: List<DayActivity> = emptyList(),
    val consistency: List<DayActivity> = emptyList(),
    val forecast: List<Pair<Long, Int>> = emptyList(),
    val totalWords: Int = 0,
    val statusCounts: Map<LearningStatus, Int> = emptyMap(),
    val totalReviews: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val goalsMet: Int = 0,
    val recall: RecallCounts = RecallCounts(0, 0),
    val recallRate: Double? = null,
    val history: List<HistoryRow> = emptyList()
) {
    val hasReviews: Boolean get() = totalReviews > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val study: StudyRepository,
    words: WordRepository,
    private val clock: Clock
) : ViewModel() {
    private val range = MutableStateFlow(ActivityRange.WEEK)

    private val nowFlow: Flow<Long> = clock.ticks(60_000L)
    private val todayFlow = nowFlow.map { LearningDay.of(it, clock.zone) }.distinctUntilChanged()
    private val recallFlow = todayFlow.flatMapLatest { study.observeRecall(it - RECALL_WINDOW_DAYS + 1) }

    private data class Facts(
        val activity: List<DayActivity>,
        val recall: RecallCounts,
        val history: List<HistoryRow>
    )

    private val facts = combine(
        study.observeDayActivity(),
        recallFlow,
        study.observeRecentHistory(HISTORY_LIMIT)
    ) { activity, recall, history -> Facts(activity, recall, history) }

    val state: StateFlow<ProgressUiState> = combine(
        facts,
        study.observeCandidates(),
        words.observeWords(),
        range,
        nowFlow
    ) { fact, candidates, allWords, currentRange, now ->
        val today = LearningDay.of(now, clock.zone)
        val streaks = ProgressCalculator.streaks(fact.activity, today)
        val month = ProgressCalculator.fillDays(fact.activity, today - RECALL_WINDOW_DAYS + 1, today)
        ProgressUiState(
            isLoading = false,
            today = today,
            range = currentRange,
            activity = ProgressCalculator.fillDays(fact.activity, today - currentRange.days + 1, today),
            consistency = ProgressCalculator.fillDays(fact.activity, today - CONSISTENCY_DAYS + 1, today),
            forecast = ProgressCalculator.forecast(candidates, now, clock.zone, FORECAST_DAYS),
            totalWords = allWords.size,
            statusCounts = allWords
                .mapNotNull { it.card?.toMemory()?.status() }
                .groupingBy { it }
                .eachCount(),
            totalReviews = fact.activity.sumOf { it.reviews },
            currentStreak = streaks.current,
            bestStreak = streaks.best,
            goalsMet = month.count { it.goalMet },
            recall = fact.recall,
            recallRate = ProgressCalculator.recallRate(fact.recall),
            history = fact.history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setRange(value: ActivityRange) {
        range.value = value
    }

    companion object {
        const val RECALL_WINDOW_DAYS = 30
        const val CONSISTENCY_DAYS = 112
        const val FORECAST_DAYS = 7
        const val HISTORY_LIMIT = 20
    }
}
