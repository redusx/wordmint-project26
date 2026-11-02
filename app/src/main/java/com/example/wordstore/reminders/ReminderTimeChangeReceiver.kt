package com.example.wordstore.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wordstore.WordStoreApplication

/** The pending delay was computed for the old clock, so it has to be recomputed. */
class ReminderTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                (context.applicationContext as WordStoreApplication).container.reminderScheduler.sync()
            }
        }
    }
}
