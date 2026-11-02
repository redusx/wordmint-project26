package com.example.wordstore.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.wordstore.domain.repository.AppSettings
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reuses the file and language keys of earlier versions, so existing choices carry over. */
class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("word_store_prefs", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    private fun read() = AppSettings(
        sourceLang = prefs.getString(KEY_SOURCE, null) ?: "en",
        targetLang = prefs.getString(KEY_TARGET, null) ?: "tr",
        dailyNewGoal = prefs.getInt(KEY_GOAL, 10),
        reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 19),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
        onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false)
    )

    private inline fun change(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit(action = block)
        state.value = read()
    }

    override fun setLanguages(sourceLang: String, targetLang: String) = change {
        putString(KEY_SOURCE, sourceLang)
        putString(KEY_TARGET, targetLang)
    }

    override fun setDailyNewGoal(goal: Int) = change { putInt(KEY_GOAL, goal.coerceIn(0, MAX_GOAL)) }

    override fun setReminder(enabled: Boolean, hour: Int, minute: Int) = change {
        putBoolean(KEY_REMINDER_ENABLED, enabled)
        putInt(KEY_REMINDER_HOUR, hour)
        putInt(KEY_REMINDER_MINUTE, minute)
    }

    override fun setThemeMode(mode: ThemeMode) = change { putString(KEY_THEME, mode.name) }

    override fun setOnboardingDone() = change { putBoolean(KEY_ONBOARDING, true) }

    override var lastReminderDay: Long?
        get() = if (prefs.contains(KEY_LAST_REMINDER)) prefs.getLong(KEY_LAST_REMINDER, 0) else null
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LAST_REMINDER) else putLong(KEY_LAST_REMINDER, value)
        }

    companion object {
        const val MAX_GOAL = 50

        private const val KEY_SOURCE = "last_source_lang"
        private const val KEY_TARGET = "last_target_lang"
        private const val KEY_GOAL = "daily_new_goal"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_LAST_REMINDER = "last_reminder_day"
    }
}
