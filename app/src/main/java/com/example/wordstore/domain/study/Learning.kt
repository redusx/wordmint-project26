package com.example.wordstore.domain.study

import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.CardState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A learning day starts at 04:00 local time, so a late session counts towards the same day. */
object LearningDay {
    private const val ROLLOVER_HOUR = 4L

    fun of(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).minusHours(ROLLOVER_HOUR).toLocalDate().toEpochDay()

    fun startMillis(day: Long, zone: ZoneId): Long =
        LocalDate.ofEpochDay(day)
            .atTime(LocalTime.of(ROLLOVER_HOUR.toInt(), 0))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}

enum class LearningStatus { NEW, LEARNING, MASTERED }

const val MASTERED_STABILITY_DAYS = 21.0

fun CardMemory.status(): LearningStatus = when {
    state == CardState.NEW -> LearningStatus.NEW
    state == CardState.REVIEW && (stability ?: 0.0) >= MASTERED_STABILITY_DAYS -> LearningStatus.MASTERED
    else -> LearningStatus.LEARNING
}

/** A reviewed card whose time has come. New cards are never "due"; they wait for the daily allowance. */
fun CardMemory.isDue(nowMillis: Long): Boolean = state != CardState.NEW && dueAt <= nowMillis
