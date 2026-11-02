package com.example.wordstore.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wordstore.WordStoreApplication
import com.example.wordstore.domain.reminder.ReminderPolicy
import com.example.wordstore.domain.study.LearningDay

/** Sends today's reminder if there is something to say, then schedules tomorrow's. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as WordStoreApplication).container
        val settings = container.settings
        val now = container.clock.millis()
        val today = LearningDay.of(now, container.clock.zone)

        val waiting = container.study.loadQueue(now).size
        val shouldNotify = ReminderPolicy.shouldNotify(
            enabled = settings.settings.value.reminderEnabled,
            canPostNotifications = container.reminderNotifier.canPost(),
            waitingWords = waiting,
            lastSentDay = settings.lastReminderDay,
            today = today
        )
        if (shouldNotify) {
            container.reminderNotifier.show(waiting)
            settings.lastReminderDay = today
        }

        container.reminderScheduler.scheduleNext()
        return Result.success()
    }
}
