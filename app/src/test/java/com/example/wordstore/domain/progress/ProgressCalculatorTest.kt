package com.example.wordstore.domain.progress

import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.study.QueueCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class ProgressCalculatorTest {
    private fun active(vararg days: Long) = days.map { DayActivity(it, 3, 0, 0) }

    @Test
    fun `no reviews means no streak`() {
        assertEquals(Streaks(0, 0), ProgressCalculator.streaks(emptyList(), 100))
    }

    @Test
    fun `streak counts back from today`() {
        assertEquals(Streaks(3, 3), ProgressCalculator.streaks(active(98, 99, 100), 100))
    }

    @Test
    fun `not studying yet today keeps yesterday's streak alive`() {
        assertEquals(Streaks(2, 2), ProgressCalculator.streaks(active(98, 99), 100))
    }

    @Test
    fun `a missed day ends the streak but best is kept`() {
        assertEquals(Streaks(0, 4), ProgressCalculator.streaks(active(90, 91, 92, 93), 100))
        assertEquals(Streaks(1, 4), ProgressCalculator.streaks(active(90, 91, 92, 93, 100), 100))
    }

    @Test
    fun `days with zero reviews do not count`() {
        val activity = listOf(DayActivity(99, 0, 0, 0), DayActivity(100, 1, 0, 0))
        assertEquals(Streaks(1, 1), ProgressCalculator.streaks(activity, 100))
    }

    @Test
    fun `fill days adds the gaps and keeps real values`() {
        val filled = ProgressCalculator.fillDays(listOf(DayActivity(11, 5, 1, 10)), 10, 12)
        assertEquals(listOf(0, 5, 0), filled.map { it.reviews })
        assertEquals(listOf(10L, 11L, 12L), filled.map { it.day })
    }

    @Test
    fun `recall needs a minimum number of reviews`() {
        assertNull(ProgressCalculator.recallRate(RecallCounts(9, 9)))
        assertEquals(0.8, ProgressCalculator.recallRate(RecallCounts(10, 8))!!, 1e-9)
    }

    @Test
    fun `goal met uses the goal stored with the day`() {
        assertTrue(DayActivity(1, 20, 10, 10).goalMet)
        assertFalse(DayActivity(1, 20, 9, 10).goalMet)
        assertFalse(DayActivity(1, 20, 0, 0).goalMet)
    }

    @Test
    fun `forecast counts overdue cards as today and skips new ones`() {
        val hour = 3_600_000L
        val now = 10 * 24 * hour + 12 * hour
        val zone = ZoneOffset.UTC
        fun card(id: Long, state: CardState, due: Long) = QueueCandidate(id, state, due, 0)
        val result = ProgressCalculator.forecast(
            listOf(
                card(1, CardState.REVIEW, now - 48 * hour),
                card(2, CardState.REVIEW, now + hour),
                card(3, CardState.REVIEW, now + 24 * hour),
                card(4, CardState.NEW, now),
                card(5, CardState.REVIEW, now + 24 * 30 * hour)
            ),
            now, zone, 3
        )
        assertEquals(listOf(2, 1, 0), result.map { it.second })
    }
}
