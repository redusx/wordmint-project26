package com.example.wordstore.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wordstore.domain.reminder.ReminderPolicy
import com.example.wordstore.domain.repository.SettingsRepository
import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * Keeps exactly one pending reminder at the user's chosen local time.
 *
 * Uses a one-time request that schedules its successor rather than a periodic one: periodic work
 * drifts and cannot follow a wall-clock time across daylight saving changes.
 */
class ReminderScheduler(
    context: Context,
    private val settings: SettingsRepository,
    private val clock: Clock
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Call when settings or the clock change. Replaces whatever is pending. */
    fun sync() = enqueue(ExistingWorkPolicy.REPLACE)

    /** Call at app start. Leaves a pending reminder alone, or restores one that is missing. */
    fun ensureScheduled() = enqueue(ExistingWorkPolicy.KEEP)

    /** Call from the running worker. Replacing here would cancel the worker itself. */
    fun scheduleNext() = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(policy: ExistingWorkPolicy) {
        val current = settings.settings.value
        if (!current.reminderEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        workManager.enqueueUniqueWork(WORK_NAME, policy, buildRequest(current.reminderHour, current.reminderMinute))
    }

    private fun buildRequest(hour: Int, minute: Int): OneTimeWorkRequest {
        val now = clock.millis()
        val delay = ReminderPolicy.nextTrigger(now, clock.zone, hour, minute) - now
        return OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
    }

    companion object {
        const val WORK_NAME = "daily_study_reminder"
    }
}
