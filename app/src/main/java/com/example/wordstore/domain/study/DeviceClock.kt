package com.example.wordstore.domain.study

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Reads the device's time zone on every call.
 *
 * `Clock.systemDefaultZone()` fixes the zone when it is created, so a long-lived instance keeps
 * computing "today" and reminder times for the old zone after the user travels.
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}
