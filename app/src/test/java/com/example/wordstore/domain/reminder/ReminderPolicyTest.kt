package com.example.wordstore.domain.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")
    private val newYork = ZoneId.of("America/New_York")

    private fun millis(zone: ZoneId, y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `later today when the time has not passed`() {
        val now = millis(istanbul, 2026, 5, 10, 8, 0)
        assertEquals(millis(istanbul, 2026, 5, 10, 19, 30), ReminderPolicy.nextTrigger(now, istanbul, 19, 30))
    }

    @Test
    fun `tomorrow when the time has passed`() {
        val now = millis(istanbul, 2026, 5, 10, 20, 0)
        assertEquals(millis(istanbul, 2026, 5, 11, 19, 30), ReminderPolicy.nextTrigger(now, istanbul, 19, 30))
    }

    @Test
    fun `exactly at the time moves to tomorrow so a reminder cannot fire twice`() {
        val now = millis(istanbul, 2026, 5, 10, 19, 30)
        assertEquals(millis(istanbul, 2026, 5, 11, 19, 30), ReminderPolicy.nextTrigger(now, istanbul, 19, 30))
    }

    @Test
    fun `keeps the wall clock time across the spring DST change`() {
        val now = millis(newYork, 2026, 3, 7, 20, 0)
        val next = ReminderPolicy.nextTrigger(now, newYork, 19, 0)
        assertEquals(millis(newYork, 2026, 3, 8, 19, 0), next)
        assertEquals(Duration.ofHours(22).toMillis(), next - now)
    }

    @Test
    fun `keeps the wall clock time across the autumn DST change`() {
        val now = millis(newYork, 2026, 10, 31, 20, 0)
        val next = ReminderPolicy.nextTrigger(now, newYork, 19, 0)
        assertEquals(Duration.ofHours(24).toMillis(), next - now)
    }

    @Test
    fun `a change of zone changes the instant`() {
        val now = millis(istanbul, 2026, 5, 10, 8, 0)
        val inIstanbul = ReminderPolicy.nextTrigger(now, istanbul, 19, 0)
        val inNewYork = ReminderPolicy.nextTrigger(now, newYork, 19, 0)
        assertEquals(Duration.ofHours(7).toMillis(), inNewYork - inIstanbul)
    }

    @Test
    fun `notifies only when enabled, permitted, useful and not yet sent today`() {
        assertTrue(ReminderPolicy.shouldNotify(true, true, 5, lastSentDay = 99, today = 100))
        assertTrue(ReminderPolicy.shouldNotify(true, true, 1, lastSentDay = null, today = 100))
        assertFalse(ReminderPolicy.shouldNotify(false, true, 5, 99, 100))
        assertFalse(ReminderPolicy.shouldNotify(true, false, 5, 99, 100))
        assertFalse(ReminderPolicy.shouldNotify(true, true, 0, 99, 100))
        assertFalse(ReminderPolicy.shouldNotify(true, true, 5, 100, 100))
    }
}

class DeviceClockTest {
    @org.junit.Test
    fun `follows a change of the default time zone`() {
        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Istanbul"))
            assertEquals(ZoneId.of("Europe/Istanbul"), com.example.wordstore.domain.study.DeviceClock.zone)
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            assertEquals(ZoneId.of("America/New_York"), com.example.wordstore.domain.study.DeviceClock.zone)
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }
}
