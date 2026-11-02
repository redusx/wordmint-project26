package com.example.wordstore.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wordstore.data.local.database.AppDatabase
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.repository.RoomStudyRepository
import com.example.wordstore.data.repository.RoomWordRepository
import com.example.wordstore.domain.repository.AppSettings
import com.example.wordstore.domain.repository.SaveWordResult
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.ThemeMode
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Fsrs6Scheduler
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.study.LearningDay
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyPersistenceTest {
    private val now = Instant.parse("2026-03-10T12:00:00Z").toEpochMilli()
    private val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
    private val scheduler = Fsrs6Scheduler()
    private lateinit var db: AppDatabase
    private lateinit var words: RoomWordRepository
    private lateinit var study: RoomStudyRepository

    private class Settings(goal: Int) : SettingsRepository {
        override val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings(dailyNewGoal = goal))
        override var lastReminderDay: Long? = null
        override fun setLanguages(sourceLang: String, targetLang: String) = Unit
        override fun setDailyNewGoal(goal: Int) = Unit
        override fun setReminder(enabled: Boolean, hour: Int, minute: Int) = Unit
        override fun setThemeMode(mode: ThemeMode) = Unit
        override fun setOnboardingDone() = Unit
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        words = RoomWordRepository(db.wordDao())
        study = RoomStudyRepository(db.studyDao(), scheduler, Settings(goal = 2), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun newWord(term: String, source: String = "en", target: String = "tr") =
        Word(term = term, translation = "t", sourceLang = source, targetLang = target, createdAt = now, updatedAt = now)

    private suspend fun saved(term: String) = (words.save(newWord(term)) as SaveWordResult.Saved).wordId

    @Test
    fun savingAWordMakesItAvailableForStudy() = runBlocking {
        val id = saved("book")
        val queue = study.loadQueue(now)
        assertEquals(listOf(id), queue.cards.map { it.wordId })
        assertEquals(CardState.NEW, study.loadCard(id)!!.memory.state)
    }

    @Test
    fun duplicatesArePreventedPerLanguagePair() = runBlocking {
        val id = saved("book")
        assertEquals(SaveWordResult.AlreadyExists(id), words.save(newWord(" BOOK ")))
        assertTrue(words.save(newWord("book", target = "de")) is SaveWordResult.Saved)
        assertEquals(2, words.observeWords().first().size)
    }

    @Test
    fun theDatabaseItselfRejectsDuplicates() {
        runBlocking {
            db.wordDao().insertWithCard(newWord("book"))
            try {
                db.wordDao().insertWithCard(newWord("book"))
                fail("Expected a constraint failure")
            } catch (_: SQLiteConstraintException) {
            }
        }
    }

    @Test
    fun aRatingUpdatesTheScheduleAndWritesAMatchingLog() = runBlocking {
        val id = saved("book")
        val card = study.loadCard(id)!!
        val outcome = study.recordReview(card, Rating.GOOD, now, durationMillis = 4_000)!!

        val stored = study.loadCard(id)!!.memory
        assertEquals(outcome.memory, stored)
        assertEquals(CardState.LEARNING, stored.state)
        assertEquals(now + 10 * Fsrs6Scheduler.MINUTE, stored.dueAt)
        assertEquals(1, stored.reps)

        val log = db.wordDao().observeHistory(id).first().single()
        assertEquals(Rating.GOOD.value, log.rating)
        assertEquals(CardState.NEW.value, log.previousState)
        assertEquals(CardState.LEARNING.value, log.newState)
        assertEquals(now, log.previousDueAt)
        assertEquals(stored.dueAt, log.newDueAt)
        assertEquals(10 * Fsrs6Scheduler.MINUTE, log.intervalMillis)
        assertEquals(4_000L, log.durationMillis)
        assertEquals(stored.stability!!, log.newStability, 0.0)
        assertEquals(2, log.dailyNewGoal)
        assertEquals("fsrs-6", log.algorithmVersion)
        assertEquals(LearningDay.of(now, ZoneOffset.UTC), log.learningDay)
    }

    @Test
    fun aRepeatedTapOnTheSameCardIsIgnored() = runBlocking {
        val id = saved("book")
        val card = study.loadCard(id)!!
        assertNotNull(study.recordReview(card, Rating.EASY, now, 0))
        assertNull(study.recordReview(card, Rating.AGAIN, now, 0))
        assertEquals(1, db.wordDao().observeHistory(id).first().size)
        assertEquals(CardState.REVIEW, study.loadCard(id)!!.memory.state)
    }

    @Test
    fun theNewWordGoalLimitsNewCardsAcrossSessions() = runBlocking {
        val first = saved("a")
        saved("b")
        saved("c")
        assertEquals(2, study.loadQueue(now).newCards)

        study.recordReview(study.loadCard(first)!!, Rating.EASY, now, 0)
        assertEquals(1, study.loadQueue(now).newCards)
    }

    @Test
    fun dueCardsStayWhenTheGoalIsReached() = runBlocking {
        val ids = listOf(saved("a"), saved("b"))
        ids.forEach { study.recordReview(study.loadCard(it)!!, Rating.EASY, now, 0) }
        saved("c")
        val later = now + 400 * Fsrs6Scheduler.DAY
        val laterStudy = RoomStudyRepository(
            db.studyDao(), scheduler, Settings(goal = 2), Clock.fixed(Instant.ofEpochMilli(later), ZoneOffset.UTC)
        )
        val queue = laterStudy.loadQueue(later)
        assertEquals(2, queue.dueReviews)
        assertEquals(1, queue.newCards)

        val sameDay = study.loadQueue(now)
        assertEquals("the goal is used up today", 0, sameDay.newCards)
    }

    @Test
    fun deletingAWordRemovesItsCardButKeepsHistoryCounts() = runBlocking {
        val id = saved("book")
        study.recordReview(study.loadCard(id)!!, Rating.GOOD, now, 0)
        words.delete(id)

        assertNull(study.loadCard(id))
        assertEquals(0, db.studyDao().candidates().size)
        val day = LearningDay.of(now, ZoneOffset.UTC)
        assertEquals(1, study.observeDayActivity().first().single { it.day == day }.reviews)
        val row = study.observeRecentHistory(10).first().single()
        assertNull(row.wordId)
        assertNull(row.term)
    }

    @Test
    fun dayActivityCountsNewWordsAndStoresTheGoal() = runBlocking {
        val a = saved("a")
        val b = saved("b")
        study.recordReview(study.loadCard(a)!!, Rating.AGAIN, now, 0)
        study.recordReview(study.loadCard(a)!!, Rating.GOOD, now, 0)
        study.recordReview(study.loadCard(b)!!, Rating.EASY, now, 0)
        val day = study.observeDayActivity().first().single()
        assertEquals(3, day.reviews)
        assertEquals(2, day.newStarted)
        assertEquals(2, day.goal)
        assertTrue(day.goalMet)
    }

    @Test
    fun recallCountsOnlyReviewsOfOlderCards() = runBlocking {
        val id = saved("a")
        study.recordReview(study.loadCard(id)!!, Rating.EASY, now, 0)
        val dueAt = study.loadCard(id)!!.memory.dueAt
        study.recordReview(study.loadCard(id)!!, Rating.GOOD, dueAt, 0)
        val secondDue = study.loadCard(id)!!.memory.dueAt
        study.recordReview(study.loadCard(id)!!, Rating.AGAIN, secondDue, 0)
        val counts = study.observeRecall(0).first()
        assertEquals(2, counts.total)
        assertEquals(1, counts.recalled)
    }

    @Test
    fun editingKeepsSchedulingAndRejectsAClash() = runBlocking {
        val id = saved("book")
        saved("pen")
        study.recordReview(study.loadCard(id)!!, Rating.EASY, now, 0)
        val before = study.loadCard(id)!!

        val renamed = before.word.copy(term = "booklet", translation = "kitapçık", hint = "thin")
        assertEquals(SaveWordResult.Saved(id), words.update(renamed))
        val after = study.loadCard(id)!!
        assertEquals("booklet", after.word.term)
        assertEquals("booklet", after.word.termKey)
        assertEquals(before.memory, after.memory)

        val clash = words.update(after.word.copy(term = "PEN"))
        assertTrue(clash is SaveWordResult.AlreadyExists)
        assertEquals("booklet", study.loadCard(id)!!.word.term)
    }
}
