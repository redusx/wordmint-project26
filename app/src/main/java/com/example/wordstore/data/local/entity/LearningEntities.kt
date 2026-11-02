package com.example.wordstore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Fsrs6Scheduler

/** Current learning state of a word. Deleted together with the word. */
@Entity(
    tableName = "card_scheduling",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dueAt"), Index("state")]
)
data class CardSchedulingEntity(
    @PrimaryKey val wordId: Long,
    val state: Int = CardState.NEW.value,
    val step: Int? = null,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val dueAt: Long,
    val lastReviewAt: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    @ColumnInfo(defaultValue = Fsrs6Scheduler.ALGORITHM_VERSION)
    val schedulerVersion: String = Fsrs6Scheduler.ALGORITHM_VERSION
) {
    fun toMemory() = CardMemory(
        cardId = wordId,
        state = CardState.fromValue(state),
        step = step,
        stability = stability,
        difficulty = difficulty,
        dueAt = dueAt,
        lastReviewAt = lastReviewAt,
        reps = reps,
        lapses = lapses
    )

    companion object {
        fun from(memory: CardMemory, schedulerVersion: String) = CardSchedulingEntity(
            wordId = memory.cardId,
            state = memory.state.value,
            step = memory.step,
            stability = memory.stability,
            difficulty = memory.difficulty,
            dueAt = memory.dueAt,
            lastReviewAt = memory.lastReviewAt,
            reps = memory.reps,
            lapses = memory.lapses,
            schedulerVersion = schedulerVersion
        )
    }
}

/**
 * Immutable record of one self-rated review.
 *
 * [wordId] becomes null when the word is deleted, so past activity stays accurate.
 * [learningDay] is fixed at review time, so later travel cannot rewrite history.
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("wordId"), Index("learningDay")]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long?,
    val rating: Int,
    val reviewedAt: Long,
    val learningDay: Long,
    val elapsedMillis: Long,
    val durationMillis: Long,
    val previousDueAt: Long,
    val newDueAt: Long,
    val intervalMillis: Long,
    val previousState: Int,
    val newState: Int,
    val previousStep: Int?,
    val newStep: Int?,
    val previousStability: Double?,
    val newStability: Double,
    val previousDifficulty: Double?,
    val newDifficulty: Double,
    /** The goal in force when this review happened, so past goal completion stays true. */
    val dailyNewGoal: Int,
    val algorithmVersion: String
)
