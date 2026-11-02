package com.example.wordstore.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.wordstore.data.local.entity.CardSchedulingEntity
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.RecallCounts
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.scheduling.ReviewOutcome
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import kotlinx.coroutines.flow.Flow

data class CandidateRow(val wordId: Long, val state: Int, val dueAt: Long, val createdAt: Long)

data class HistoryRow(
    val id: Long,
    val wordId: Long?,
    val term: String?,
    val rating: Int,
    val reviewedAt: Long,
    val intervalMillis: Long
)

@Dao
abstract class StudyDao {
    @Query(
        """
        SELECT c.wordId AS wordId, c.state AS state, c.dueAt AS dueAt, w.createdAt AS createdAt
        FROM card_scheduling c INNER JOIN words w ON w.id = c.wordId
        """
    )
    abstract fun observeCandidates(): Flow<List<CandidateRow>>

    @Query(
        """
        SELECT c.wordId AS wordId, c.state AS state, c.dueAt AS dueAt, w.createdAt AS createdAt
        FROM card_scheduling c INNER JOIN words w ON w.id = c.wordId
        """
    )
    abstract suspend fun candidates(): List<CandidateRow>

    @Query("SELECT * FROM card_scheduling WHERE wordId = :wordId")
    abstract suspend fun card(wordId: Long): CardSchedulingEntity?

    @Query("SELECT * FROM words WHERE id = :wordId")
    abstract suspend fun word(wordId: Long): Word?

    @Query("SELECT COUNT(*) FROM review_logs WHERE learningDay = :day AND previousState = ${CARD_STATE_NEW}")
    abstract suspend fun newStartedOn(day: Long): Int

    @Query(
        """
        SELECT learningDay AS day, COUNT(*) AS reviews,
               SUM(CASE WHEN previousState = $CARD_STATE_NEW THEN 1 ELSE 0 END) AS newStarted,
               MAX(dailyNewGoal) AS goal
        FROM review_logs GROUP BY learningDay ORDER BY learningDay
        """
    )
    abstract fun observeDayActivity(): Flow<List<DayActivity>>

    @Query(
        """
        SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN rating > 1 THEN 1 ELSE 0 END), 0) AS recalled
        FROM review_logs WHERE previousState = $CARD_STATE_REVIEW AND learningDay >= :fromDay
        """
    )
    abstract fun observeRecall(fromDay: Long): Flow<RecallCounts>

    @Query(
        """
        SELECT r.id AS id, r.wordId AS wordId, w.term AS term, r.rating AS rating,
               r.reviewedAt AS reviewedAt, r.intervalMillis AS intervalMillis
        FROM review_logs r LEFT JOIN words w ON w.id = r.wordId
        ORDER BY r.reviewedAt DESC, r.id DESC LIMIT :limit
        """
    )
    abstract fun observeRecentHistory(limit: Int): Flow<List<HistoryRow>>

    @Update
    protected abstract suspend fun updateCard(card: CardSchedulingEntity)

    @Insert
    protected abstract suspend fun insertLog(log: ReviewLogEntity)

    /**
     * Applies one rating atomically: the schedule and its log entry change together.
     *
     * [expectedReps] guards against a repeated tap: if the card has moved on since the caller read
     * it, nothing is written and null is returned.
     */
    @Transaction
    open suspend fun recordReview(
        wordId: Long,
        expectedReps: Int,
        rating: Rating,
        nowMillis: Long,
        learningDay: Long,
        durationMillis: Long,
        dailyNewGoal: Int,
        scheduler: SpacedRepetitionScheduler
    ): ReviewOutcome? {
        val current = card(wordId) ?: return null
        if (current.reps != expectedReps) return null

        val previous = current.toMemory()
        val outcome = scheduler.review(previous, rating, nowMillis)
        val updated = outcome.memory

        updateCard(CardSchedulingEntity.from(updated, scheduler.algorithmVersion))
        insertLog(
            ReviewLogEntity(
                wordId = wordId,
                rating = rating.value,
                reviewedAt = nowMillis,
                learningDay = learningDay,
                elapsedMillis = outcome.elapsedMillis,
                durationMillis = durationMillis,
                previousDueAt = previous.dueAt,
                newDueAt = updated.dueAt,
                intervalMillis = outcome.intervalMillis,
                previousState = previous.state.value,
                newState = updated.state.value,
                previousStep = previous.step,
                newStep = updated.step,
                previousStability = previous.stability,
                newStability = requireNotNull(updated.stability),
                previousDifficulty = previous.difficulty,
                newDifficulty = requireNotNull(updated.difficulty),
                dailyNewGoal = dailyNewGoal,
                algorithmVersion = scheduler.algorithmVersion
            )
        )
        return outcome
    }

    private companion object {
        const val CARD_STATE_NEW = 0
        const val CARD_STATE_REVIEW = 2
    }
}

fun CandidateRow.toState() = CardState.fromValue(state)
