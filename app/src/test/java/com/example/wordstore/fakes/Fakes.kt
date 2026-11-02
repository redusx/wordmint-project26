package com.example.wordstore.fakes

import com.example.wordstore.data.local.database.HistoryRow
import com.example.wordstore.data.local.entity.CardSchedulingEntity
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.local.entity.WordWithCard
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.RecallCounts
import com.example.wordstore.domain.repository.AppSettings
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.domain.repository.LanguagePackRepository
import com.example.wordstore.domain.repository.PackEvent
import com.example.wordstore.domain.repository.SaveWordResult
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.StudyCard
import com.example.wordstore.domain.repository.StudyRepository
import com.example.wordstore.domain.repository.ThemeMode
import com.example.wordstore.domain.repository.WordRepository
import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.scheduling.ReviewOutcome
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import com.example.wordstore.domain.study.LearningDay
import com.example.wordstore.domain.study.QueueCandidate
import com.example.wordstore.domain.study.StudyQueue
import com.example.wordstore.domain.study.StudyQueuePolicy
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MutableClock(var nowMillis: Long = 1_700_000_000_000L, private val zoneId: ZoneId = ZoneId.of("UTC")) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = MutableClock(nowMillis, zone)
    override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    override fun millis(): Long = nowMillis
}

fun word(id: Long, term: String = "term$id", createdAt: Long = id, translation: String = "translation$id") = Word(
    id = id,
    term = term,
    translation = translation,
    sourceLang = "en",
    targetLang = "tr",
    createdAt = createdAt,
    updatedAt = createdAt
)

class FakeSettings(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = state.asStateFlow()
    override var lastReminderDay: Long? = null

    override fun setLanguages(sourceLang: String, targetLang: String) {
        state.value = state.value.copy(sourceLang = sourceLang, targetLang = targetLang)
    }
    override fun setDailyNewGoal(goal: Int) { state.value = state.value.copy(dailyNewGoal = goal) }
    override fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        state.value = state.value.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute)
    }
    override fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
    override fun setOnboardingDone() { state.value = state.value.copy(onboardingDone = true) }
}

/** An in-memory study store that applies the real queue policy and the real scheduler. */
class FakeStudyRepository(
    private val scheduler: SpacedRepetitionScheduler,
    private val clock: Clock,
    words: List<Word>,
    private val dailyNewGoal: Int = 10
) : StudyRepository {
    val wordsById = words.associateBy { it.id }
    val memories = words.associate { it.id to CardMemory(cardId = it.id, dueAt = it.createdAt) }.toMutableMap()
    val recorded = mutableListOf<Pair<Long, Rating>>()
    private var newStarted = 0

    override fun observeCandidates(): Flow<List<QueueCandidate>> = flowOf(candidates())
    override fun observeDayActivity(): Flow<List<DayActivity>> = flowOf(emptyList())
    override fun observeRecall(fromDay: Long): Flow<RecallCounts> = flowOf(RecallCounts(0, 0))
    override fun observeRecentHistory(limit: Int): Flow<List<HistoryRow>> = flowOf(emptyList())

    private fun candidates() = memories.values.map {
        QueueCandidate(it.cardId, it.state, it.dueAt, wordsById.getValue(it.cardId).createdAt)
    }

    override suspend fun loadQueue(nowMillis: Long): StudyQueue =
        StudyQueuePolicy.build(candidates(), nowMillis, newStarted, dailyNewGoal)

    override suspend fun loadCard(wordId: Long): StudyCard? =
        wordsById[wordId]?.let { StudyCard(it, memories.getValue(wordId)) }

    override suspend fun recordReview(card: StudyCard, rating: Rating, nowMillis: Long, durationMillis: Long): ReviewOutcome? {
        val current = memories.getValue(card.word.id)
        if (current.reps != card.memory.reps) return null
        val outcome = scheduler.review(current, rating, nowMillis)
        if (current.state == CardState.NEW) newStarted++
        memories[card.word.id] = outcome.memory
        recorded += card.word.id to rating
        LearningDay.of(nowMillis, clock.zone)
        return outcome
    }
}

class FakeWordRepository(initial: List<WordWithCard> = emptyList()) : WordRepository {
    val words = MutableStateFlow(initial)
    private var nextId = 1000L

    override fun observeWords(): Flow<List<WordWithCard>> = words
    override fun observeWord(id: Long): Flow<WordWithCard?> = words.map { list -> list.firstOrNull { it.word.id == id } }
    override fun observeHistory(wordId: Long): Flow<List<ReviewLogEntity>> = flowOf(emptyList())
    override fun observeMatch(term: String, sourceLang: String, targetLang: String): Flow<Word?> =
        words.map { list ->
            list.map { it.word }.firstOrNull {
                it.termKey == Word.termKeyOf(term) && it.sourceLang == sourceLang && it.targetLang == targetLang
            }
        }

    override suspend fun save(word: Word): SaveWordResult {
        words.value.firstOrNull { it.word.termKey == word.termKey }?.let { return SaveWordResult.AlreadyExists(it.word.id) }
        val id = nextId++
        words.value = words.value + WordWithCard(word.copy(id = id), CardSchedulingEntity(wordId = id, dueAt = word.createdAt))
        return SaveWordResult.Saved(id)
    }

    override suspend fun update(word: Word): SaveWordResult {
        words.value = words.value.map { if (it.word.id == word.id) it.copy(word = word) else it }
        return SaveWordResult.Saved(word.id)
    }

    override suspend fun delete(wordId: Long) {
        words.value = words.value.filterNot { it.word.id == wordId }
    }
}

class FakePacks(downloaded: Set<String> = setOf("en", "tr")) : LanguagePackRepository {
    private val state = MutableStateFlow(listOf("en", "tr", "de").map { LanguagePack(it, it in downloaded, false) })
    val requested = mutableListOf<Pair<String, Boolean>>()
    override val packs: StateFlow<List<LanguagePack>> = state
    override val events: Flow<PackEvent> = emptyFlow()
    override suspend fun refresh() = Unit
    override fun download(code: String, allowCellular: Boolean) { requested += code to allowCellular }
    override fun isOnUnmeteredNetwork() = true
}
