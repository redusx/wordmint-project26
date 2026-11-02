package com.example.wordstore.domain.scheduling

/** Self-assessed recall. Values match the FSRS rating scale. */
enum class Rating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    companion object {
        fun fromValue(value: Int): Rating = entries.first { it.value == value }
    }
}

/** Values 1 to 3 match the FSRS reference. [NEW] is a word that has never been reviewed. */
enum class CardState(val value: Int) {
    NEW(0),
    LEARNING(1),
    REVIEW(2),
    RELEARNING(3);

    companion object {
        fun fromValue(value: Int): CardState = entries.first { it.value == value }
    }
}

/** The scheduling state of one word, free of any storage or UI concern. */
data class CardMemory(
    val cardId: Long,
    val state: CardState = CardState.NEW,
    val step: Int? = null,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val dueAt: Long,
    val lastReviewAt: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0
)

data class ReviewOutcome(
    val rating: Rating,
    val memory: CardMemory,
    val intervalMillis: Long,
    val elapsedMillis: Long
)

/** The UI and repositories depend on this, never on a specific algorithm. */
interface SpacedRepetitionScheduler {
    val algorithmVersion: String

    fun review(memory: CardMemory, rating: Rating, nowMillis: Long): ReviewOutcome

    /** What each rating would do. Same computation as [review], so estimates are exact. */
    fun preview(memory: CardMemory, nowMillis: Long): Map<Rating, ReviewOutcome> =
        Rating.entries.associateWith { review(memory, it, nowMillis) }

    /** Predicted probability of recall at [nowMillis], from 0 to 1. Zero if never reviewed. */
    fun retrievability(memory: CardMemory, nowMillis: Long): Double
}
