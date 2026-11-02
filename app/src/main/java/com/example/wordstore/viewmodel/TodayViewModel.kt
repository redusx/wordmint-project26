package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.ProgressCalculator
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.StudyRepository
import com.example.wordstore.domain.repository.WordRepository
import com.example.wordstore.domain.study.LearningDay
import com.example.wordstore.domain.study.StudyQueuePolicy
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val isLoading: Boolean = true,
    val nowMillis: Long = 0,
    val totalWords: Int = 0,
    val waiting: Int = 0,
    val dueReviews: Int = 0,
    val inSteps: Int = 0,
    val newCards: Int = 0,
    val newUnstarted: Int = 0,
    val reviewsToday: Int = 0,
    val newStartedToday: Int = 0,
    val dailyNewGoal: Int = 0,
    val currentStreak: Int = 0,
    val nextDueAt: Long? = null,
    val recentWords: List<Word> = emptyList()
) {
    /** Share of today's known work that is done. */
    val level: Float
        get() = if (reviewsToday + waiting == 0) 0f else reviewsToday.toFloat() / (reviewsToday + waiting)

    val hasStartedToday: Boolean get() = reviewsToday > 0
    val hasWork: Boolean get() = waiting > 0
    val todayTotal: Int get() = reviewsToday + waiting
}

class TodayViewModel(
    study: StudyRepository,
    words: WordRepository,
    settings: SettingsRepository,
    private val clock: Clock
) : ViewModel() {

    val state: StateFlow<TodayUiState> = combine(
        study.observeCandidates(),
        study.observeDayActivity(),
        settings.settings,
        words.observeWords(),
        clock.ticks()
    ) { candidates, activity, appSettings, allWords, now ->
        val today = LearningDay.of(now, clock.zone)
        val todayActivity: DayActivity? = activity.firstOrNull { it.day == today }
        val newStarted = todayActivity?.newStarted ?: 0
        val queue = StudyQueuePolicy.build(candidates, now, newStarted, appSettings.dailyNewGoal)
        TodayUiState(
            isLoading = false,
            nowMillis = now,
            totalWords = allWords.size,
            waiting = queue.size,
            dueReviews = queue.dueReviews,
            inSteps = queue.dueLearning + queue.learningSoon,
            newCards = queue.newCards,
            newUnstarted = candidates.count { it.state == com.example.wordstore.domain.scheduling.CardState.NEW },
            reviewsToday = todayActivity?.reviews ?: 0,
            newStartedToday = newStarted,
            dailyNewGoal = appSettings.dailyNewGoal,
            currentStreak = ProgressCalculator.streaks(activity, today).current,
            nextDueAt = queue.nextDueAt,
            recentWords = allWords.take(RECENT_WORDS).map { it.word }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private companion object {
        const val RECENT_WORDS = 3
    }
}
