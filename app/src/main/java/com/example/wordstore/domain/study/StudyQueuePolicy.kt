package com.example.wordstore.domain.study

import com.example.wordstore.domain.scheduling.CardState

data class QueueCandidate(
    val wordId: Long,
    val state: CardState,
    val dueAt: Long,
    val createdAt: Long
)

data class StudyQueue(
    val cards: List<QueueCandidate>,
    val dueReviews: Int,
    val dueLearning: Int,
    val newCards: Int,
    val learningSoon: Int,
    /** When the next card outside this queue becomes due. Null if no such card exists. */
    val nextDueAt: Long?
) {
    val size: Int get() = cards.size
    val isEmpty: Boolean get() = cards.isEmpty()
}

/**
 * Decides what to study and in what order. Pure and deterministic.
 *
 * Due cards are never limited. The daily goal limits only how many new words are started.
 */
object StudyQueuePolicy {
    /** Learning cards due this soon are shown at the end so same-day steps finish in one session. */
    const val LEARN_AHEAD_MILLIS = 20 * 60_000L

    fun build(
        candidates: List<QueueCandidate>,
        nowMillis: Long,
        newStartedToday: Int,
        dailyNewGoal: Int
    ): StudyQueue {
        val byDue = compareBy<QueueCandidate>({ it.dueAt }, { it.wordId })

        val dueReviews = candidates
            .filter { it.state == CardState.REVIEW && it.dueAt <= nowMillis }
            .sortedWith(byDue)

        val inSteps = candidates.filter { it.state == CardState.LEARNING || it.state == CardState.RELEARNING }
        val dueLearning = inSteps.filter { it.dueAt <= nowMillis }.sortedWith(byDue)
        val learningSoon = inSteps
            .filter { it.dueAt > nowMillis && it.dueAt <= nowMillis + LEARN_AHEAD_MILLIS }
            .sortedWith(byDue)

        val newAllowance = (dailyNewGoal - newStartedToday).coerceAtLeast(0)
        val newCards = candidates
            .filter { it.state == CardState.NEW }
            .sortedWith(compareBy({ it.createdAt }, { it.wordId }))
            .take(newAllowance)

        val queued = dueReviews + dueLearning + newCards + learningSoon
        val queuedIds = queued.mapTo(HashSet()) { it.wordId }
        val nextDueAt = candidates
            .filter { it.state != CardState.NEW && it.wordId !in queuedIds }
            .minOfOrNull { it.dueAt }

        return StudyQueue(
            cards = queued,
            dueReviews = dueReviews.size,
            dueLearning = dueLearning.size,
            newCards = newCards.size,
            learningSoon = learningSoon.size,
            nextDueAt = nextDueAt
        )
    }
}
