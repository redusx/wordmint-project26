package com.example.wordstore.domain.progress

import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.study.LearningDay
import com.example.wordstore.domain.study.QueueCandidate
import java.time.ZoneId

data class DayActivity(
    val day: Long,
    val reviews: Int,
    val newStarted: Int,
    val goal: Int
) {
    /** A goal of zero is never counted as met: no target was set. */
    val goalMet: Boolean get() = goal > 0 && newStarted >= goal
}

data class RecallCounts(val total: Int, val recalled: Int)

data class Streaks(val current: Int, val best: Int)

/** Pure calculations over stored review data. Nothing here estimates or invents a value. */
object ProgressCalculator {
    const val MIN_REVIEWS_FOR_RECALL = 10

    fun streaks(activity: List<DayActivity>, today: Long): Streaks {
        val days = activity.filter { it.reviews > 0 }.map { it.day }.distinct().sorted()
        if (days.isEmpty()) return Streaks(0, 0)

        var best = 1
        var run = 1
        for (index in 1 until days.size) {
            run = if (days[index] == days[index - 1] + 1) run + 1 else 1
            best = maxOf(best, run)
        }

        val daySet = days.toHashSet()
        // Today not studied yet does not break a streak; yesterday still counts.
        var cursor = if (today in daySet) today else today - 1
        var current = 0
        while (cursor in daySet) {
            current++
            cursor--
        }
        return Streaks(current, best)
    }

    /** One entry per day from [firstDay] to [lastDay], with empty days filled in. */
    fun fillDays(activity: List<DayActivity>, firstDay: Long, lastDay: Long): List<DayActivity> {
        val byDay = activity.associateBy { it.day }
        return (firstDay..lastDay).map { day -> byDay[day] ?: DayActivity(day, 0, 0, 0) }
    }

    /** Share of reviews not rated Again, or null with too few reviews to mean anything. */
    fun recallRate(counts: RecallCounts): Double? =
        if (counts.total < MIN_REVIEWS_FOR_RECALL) null else counts.recalled.toDouble() / counts.total

    /** Reviewed words coming due on each of the next [days] learning days, starting today. */
    fun forecast(
        candidates: List<QueueCandidate>,
        nowMillis: Long,
        zone: ZoneId,
        days: Int
    ): List<Pair<Long, Int>> {
        val today = LearningDay.of(nowMillis, zone)
        val counts = IntArray(days)
        for (card in candidates) {
            if (card.state == CardState.NEW) continue
            val offset = (LearningDay.of(maxOf(card.dueAt, nowMillis), zone) - today).toInt()
            if (offset in 0 until days) counts[offset]++
        }
        return counts.mapIndexed { index, count -> today + index to count }
    }
}
