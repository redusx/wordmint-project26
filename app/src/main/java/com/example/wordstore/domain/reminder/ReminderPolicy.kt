package com.example.wordstore.domain.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object ReminderPolicy {
    /** The next wall-clock [hour]:[minute] strictly after [nowMillis], correct across DST changes. */
    fun nextTrigger(nowMillis: Long, zone: ZoneId, hour: Int, minute: Int): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.with(LocalTime.of(hour, minute))
        val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(zone)
        return next.toInstant().toEpochMilli()
    }

    /** Whether a reminder is worth sending. Only one per learning day, and only with work to do. */
    fun shouldNotify(
        enabled: Boolean,
        canPostNotifications: Boolean,
        waitingWords: Int,
        lastSentDay: Long?,
        today: Long
    ): Boolean = enabled && canPostNotifications && waitingWords > 0 && lastSentDay != today
}
