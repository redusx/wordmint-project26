package com.example.wordstore.domain.repository

import com.example.wordstore.data.local.database.HistoryRow
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.local.entity.WordWithCard
import com.example.wordstore.domain.model.LookupResult
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.progress.RecallCounts
import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.scheduling.ReviewOutcome
import com.example.wordstore.domain.study.QueueCandidate
import com.example.wordstore.domain.study.StudyQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface SaveWordResult {
    data class Saved(val wordId: Long) : SaveWordResult
    data class AlreadyExists(val wordId: Long) : SaveWordResult
}

interface WordRepository {
    fun observeWords(): Flow<List<WordWithCard>>
    fun observeWord(id: Long): Flow<WordWithCard?>
    fun observeHistory(wordId: Long): Flow<List<ReviewLogEntity>>
    fun observeMatch(term: String, sourceLang: String, targetLang: String): Flow<Word?>
    suspend fun save(word: Word): SaveWordResult
    suspend fun update(word: Word): SaveWordResult
    suspend fun delete(wordId: Long)
}

data class StudyCard(val word: Word, val memory: CardMemory)

interface StudyRepository {
    fun observeCandidates(): Flow<List<QueueCandidate>>
    fun observeDayActivity(): Flow<List<DayActivity>>
    fun observeRecall(fromDay: Long): Flow<RecallCounts>
    fun observeRecentHistory(limit: Int): Flow<List<HistoryRow>>

    suspend fun loadQueue(nowMillis: Long): StudyQueue
    suspend fun loadCard(wordId: Long): StudyCard?

    /** Null when the card has already moved on, for example after a repeated tap. */
    suspend fun recordReview(card: StudyCard, rating: Rating, nowMillis: Long, durationMillis: Long): ReviewOutcome?
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val sourceLang: String = "en",
    val targetLang: String = "tr",
    val dailyNewGoal: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false
)

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setLanguages(sourceLang: String, targetLang: String)
    fun setDailyNewGoal(goal: Int)
    fun setReminder(enabled: Boolean, hour: Int, minute: Int)
    fun setThemeMode(mode: ThemeMode)
    fun setOnboardingDone()
    var lastReminderDay: Long?
}

interface DictionaryRepository {
    suspend fun lookup(query: String, fromLang: String, learningLang: String, knownLang: String): LookupResult
}

data class LanguagePack(val code: String, val isDownloaded: Boolean, val isDownloading: Boolean)

sealed interface PackEvent {
    data class Downloaded(val code: String) : PackEvent
    data class Failed(val code: String, val reason: PackFailure) : PackEvent
}

enum class PackFailure { NETWORK, WIFI_REQUIRED, STORAGE, BUSY, TIMEOUT, UNKNOWN }

interface LanguagePackRepository {
    val packs: StateFlow<List<LanguagePack>>
    val events: Flow<PackEvent>
    suspend fun refresh()
    fun download(code: String, allowCellular: Boolean)
    fun isOnUnmeteredNetwork(): Boolean
}
