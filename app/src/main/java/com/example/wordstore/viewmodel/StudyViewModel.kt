package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.domain.model.contextHint
import com.example.wordstore.domain.repository.StudyCard
import com.example.wordstore.domain.repository.StudyRepository
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RatingOption(val rating: Rating, val intervalMillis: Long)

data class RatingFeedback(val rating: Rating, val intervalMillis: Long)

data class SessionSummary(
    val reviewed: Int,
    val newStarted: Int,
    val activeMillis: Long,
    val ratings: Map<Rating, Int>,
    val nextDueAt: Long?
)

sealed interface StudyPhase {
    data object Loading : StudyPhase
    data class Empty(val nextDueAt: Long?) : StudyPhase
    data class Studying(
        val card: StudyCard,
        val isRevealed: Boolean,
        val isHintShown: Boolean,
        val hint: String?,
        val options: List<RatingOption>,
        val isSaving: Boolean
    ) : StudyPhase
    data class Complete(val summary: SessionSummary) : StudyPhase
    data class Failed(val canRetry: Boolean = true) : StudyPhase
}

data class StudyUiState(
    val phase: StudyPhase = StudyPhase.Loading,
    val position: Int = 0,
    val total: Int = 0,
    val feedback: RatingFeedback? = null
) {
    /** Reviewed so far as a share of everything known about this session. */
    val level: Float get() = if (total == 0) 0f else (position - 1).coerceAtLeast(0).toFloat() / total
}

/**
 * Runs one study session. The queue is rebuilt from the database after every rating, so leaving at
 * any moment loses nothing and returning simply continues.
 */
class StudyViewModel(
    private val repository: StudyRepository,
    private val scheduler: SpacedRepetitionScheduler,
    private val clock: Clock
) : ViewModel() {

    private val mutableState = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = mutableState.asStateFlow()

    private var reviewed = 0
    private var newStarted = 0
    private var activeMillis = 0L
    private val ratings = mutableMapOf<Rating, Int>()
    private var shownAt = 0L

    init {
        loadNext()
    }

    fun retry() = loadNext()

    private fun loadNext(feedback: RatingFeedback? = null) {
        viewModelScope.launch {
            try {
                val now = clock.millis()
                val queue = repository.loadQueue(now)
                val next = queue.cards.firstOrNull()?.let { repository.loadCard(it.wordId) }
                if (next == null) {
                    mutableState.value = StudyUiState(
                        phase = if (reviewed > 0) {
                            StudyPhase.Complete(
                                SessionSummary(reviewed, newStarted, activeMillis, ratings.toMap(), queue.nextDueAt)
                            )
                        } else {
                            StudyPhase.Empty(queue.nextDueAt)
                        },
                        position = reviewed,
                        total = reviewed,
                        feedback = feedback
                    )
                    return@launch
                }
                shownAt = now
                mutableState.value = StudyUiState(
                    phase = StudyPhase.Studying(
                        card = next,
                        isRevealed = false,
                        isHintShown = false,
                        hint = next.word.contextHint(),
                        options = emptyList(),
                        isSaving = false
                    ),
                    position = reviewed + 1,
                    total = reviewed + queue.size,
                    feedback = feedback
                )
            } catch (_: android.database.SQLException) {
                mutableState.update { it.copy(phase = StudyPhase.Failed()) }
            }
        }
    }

    fun showHint() = updateStudying { it.copy(isHintShown = true) }

    fun reveal() {
        val now = clock.millis()
        updateStudying { studying ->
            if (studying.isRevealed) return@updateStudying studying
            val preview = scheduler.preview(studying.card.memory, now)
            studying.copy(
                isRevealed = true,
                options = Rating.entries.map { RatingOption(it, preview.getValue(it).intervalMillis) }
            )
        }
        mutableState.update { it.copy(feedback = null) }
    }

    fun rate(rating: Rating) {
        val studying = mutableState.value.phase as? StudyPhase.Studying ?: return
        if (!studying.isRevealed || studying.isSaving) return
        updateStudying { it.copy(isSaving = true) }

        viewModelScope.launch {
            val now = clock.millis()
            val duration = (now - shownAt).coerceIn(0, MAX_CARD_MILLIS)
            try {
                val outcome = repository.recordReview(studying.card, rating, now, duration)
                if (outcome != null) {
                    reviewed++
                    if (studying.card.memory.state == CardState.NEW) newStarted++
                    activeMillis += duration
                    ratings[rating] = (ratings[rating] ?: 0) + 1
                    loadNext(RatingFeedback(rating, outcome.intervalMillis))
                } else {
                    loadNext()
                }
            } catch (_: android.database.SQLException) {
                mutableState.update { it.copy(phase = StudyPhase.Failed()) }
            }
        }
    }

    private fun updateStudying(transform: (StudyPhase.Studying) -> StudyPhase.Studying) {
        mutableState.update { current ->
            val studying = current.phase as? StudyPhase.Studying ?: return@update current
            current.copy(phase = transform(studying))
        }
    }

    private companion object {
        /** A card left on screen while the user walked away should not count as study time. */
        const val MAX_CARD_MILLIS = 5 * 60_000L
    }
}
