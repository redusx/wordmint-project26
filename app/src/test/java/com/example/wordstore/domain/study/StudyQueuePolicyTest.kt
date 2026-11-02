package com.example.wordstore.domain.study

import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.CardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class StudyQueuePolicyTest {
    private val now = 1_000_000_000L
    private val minute = 60_000L

    private fun card(id: Long, state: CardState, dueOffset: Long = 0, createdAt: Long = id) =
        QueueCandidate(id, state, now + dueOffset, createdAt)

    private fun ids(queue: StudyQueue) = queue.cards.map { it.wordId }

    @Test
    fun `orders reviews then learning then new then learning ahead`() {
        val queue = StudyQueuePolicy.build(
            listOf(
                card(1, CardState.NEW),
                card(2, CardState.LEARNING, -5 * minute),
                card(3, CardState.REVIEW, -1 * minute),
                card(4, CardState.RELEARNING, 5 * minute),
                card(5, CardState.REVIEW, -9 * minute)
            ),
            now, 0, 10
        )
        assertEquals(listOf(5L, 3L, 2L, 1L, 4L), ids(queue))
        assertEquals(2, queue.dueReviews)
        assertEquals(1, queue.dueLearning)
        assertEquals(1, queue.newCards)
        assertEquals(1, queue.learningSoon)
    }

    @Test
    fun `daily goal limits new cards only`() {
        val candidates = (1L..20L).map { card(it, CardState.REVIEW, -minute) } +
            (21L..40L).map { card(it, CardState.NEW) }
        val queue = StudyQueuePolicy.build(candidates, now, newStartedToday = 8, dailyNewGoal = 10)
        assertEquals(20, queue.dueReviews)
        assertEquals(2, queue.newCards)
        assertEquals(listOf(21L, 22L), ids(queue).takeLast(2))
    }

    @Test
    fun `due cards remain when the new card allowance is used up`() {
        val queue = StudyQueuePolicy.build(
            listOf(card(1, CardState.REVIEW, -minute), card(2, CardState.NEW)),
            now, newStartedToday = 50, dailyNewGoal = 10
        )
        assertEquals(listOf(1L), ids(queue))
    }

    @Test
    fun `a goal of zero starts no new words`() {
        val queue = StudyQueuePolicy.build(listOf(card(1, CardState.NEW)), now, 0, 0)
        assertTrue(queue.isEmpty)
    }

    @Test
    fun `new cards come oldest first and ties break on id`() {
        val queue = StudyQueuePolicy.build(
            listOf(
                card(3, CardState.NEW, createdAt = 5),
                card(1, CardState.NEW, createdAt = 9),
                card(2, CardState.NEW, createdAt = 5)
            ),
            now, 0, 10
        )
        assertEquals(listOf(2L, 3L, 1L), ids(queue))
    }

    @Test
    fun `learning cards due beyond the window are left out and reported as next due`() {
        val far = 21 * minute
        val queue = StudyQueuePolicy.build(listOf(card(1, CardState.LEARNING, far)), now, 0, 10)
        assertTrue(queue.isEmpty)
        assertEquals(now + far, queue.nextDueAt)
    }

    @Test
    fun `future review is not due and next due is reported`() {
        val queue = StudyQueuePolicy.build(listOf(card(1, CardState.REVIEW, 3 * minute)), now, 0, 10)
        assertTrue(queue.isEmpty)
        assertEquals(now + 3 * minute, queue.nextDueAt)
    }

    @Test
    fun `no reviewed cards means no next due time`() {
        assertNull(StudyQueuePolicy.build(listOf(card(1, CardState.NEW)), now, 10, 10).nextDueAt)
    }

    @Test
    fun `same input gives the same queue`() {
        val candidates = (1L..50L).map { card(it, CardState.entries[(it % 4).toInt()], (it % 7) * minute - 3 * minute) }
        assertEquals(
            StudyQueuePolicy.build(candidates, now, 2, 10),
            StudyQueuePolicy.build(candidates.shuffled(), now, 2, 10)
        )
    }

    @Test
    fun `status follows state and stability`() {
        val base = CardMemory(cardId = 1, dueAt = now)
        assertEquals(LearningStatus.NEW, base.status())
        assertEquals(LearningStatus.LEARNING, base.copy(state = CardState.RELEARNING, stability = 90.0).status())
        assertEquals(LearningStatus.LEARNING, base.copy(state = CardState.REVIEW, stability = 20.9).status())
        assertEquals(LearningStatus.MASTERED, base.copy(state = CardState.REVIEW, stability = 21.0).status())
    }

    @Test
    fun `new cards are never due`() {
        val base = CardMemory(cardId = 1, dueAt = now - minute)
        assertFalse(base.isDue(now))
        assertTrue(base.copy(state = CardState.REVIEW).isDue(now))
    }

    @Test
    fun `learning day rolls over at four in the morning local time`() {
        val zone = ZoneId.of("Europe/Istanbul")
        fun at(hour: Int, day: Int) = ZonedDateTime.of(2026, 3, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()
        val march10 = LocalDate.of(2026, 3, 10).toEpochDay()
        assertEquals(march10, LearningDay.of(at(4, 10), zone))
        assertEquals(march10, LearningDay.of(at(23, 10), zone))
        assertEquals(march10, LearningDay.of(at(3, 11), zone))
        assertEquals(march10 + 1, LearningDay.of(at(4, 11), zone))
        assertEquals(at(4, 10), LearningDay.startMillis(march10, zone))
    }
}
