package com.example.wordstore.data.repository

import com.example.wordstore.data.local.database.CandidateRow
import com.example.wordstore.data.local.database.HistoryRow
import com.example.wordstore.data.local.database.StudyDao
import com.example.wordstore.data.local.database.toState
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.RecallCounts
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.StudyCard
import com.example.wordstore.domain.repository.StudyRepository
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.scheduling.ReviewOutcome
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import com.example.wordstore.domain.study.LearningDay
import com.example.wordstore.domain.study.QueueCandidate
import com.example.wordstore.domain.study.StudyQueue
import com.example.wordstore.domain.study.StudyQueuePolicy
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStudyRepository(
    private val dao: StudyDao,
    private val scheduler: SpacedRepetitionScheduler,
    private val settings: SettingsRepository,
    private val clock: Clock
) : StudyRepository {

    override fun observeCandidates(): Flow<List<QueueCandidate>> =
        dao.observeCandidates().map { rows -> rows.map(CandidateRow::toCandidate) }

    override fun observeDayActivity(): Flow<List<DayActivity>> = dao.observeDayActivity()

    override fun observeRecall(fromDay: Long): Flow<RecallCounts> = dao.observeRecall(fromDay)

    override fun observeRecentHistory(limit: Int): Flow<List<HistoryRow>> = dao.observeRecentHistory(limit)

    override suspend fun loadQueue(nowMillis: Long): StudyQueue =
        StudyQueuePolicy.build(
            candidates = dao.candidates().map(CandidateRow::toCandidate),
            nowMillis = nowMillis,
            newStartedToday = dao.newStartedOn(LearningDay.of(nowMillis, clock.zone)),
            dailyNewGoal = settings.settings.value.dailyNewGoal
        )

    override suspend fun loadCard(wordId: Long): StudyCard? {
        val word = dao.word(wordId) ?: return null
        val card = dao.card(wordId) ?: return null
        return StudyCard(word, card.toMemory())
    }

    override suspend fun recordReview(
        card: StudyCard,
        rating: Rating,
        nowMillis: Long,
        durationMillis: Long
    ): ReviewOutcome? = dao.recordReview(
        wordId = card.word.id,
        expectedReps = card.memory.reps,
        rating = rating,
        nowMillis = nowMillis,
        learningDay = LearningDay.of(nowMillis, clock.zone),
        durationMillis = durationMillis,
        dailyNewGoal = settings.settings.value.dailyNewGoal,
        scheduler = scheduler
    )
}

private fun CandidateRow.toCandidate() = QueueCandidate(wordId, toState(), dueAt, createdAt)
