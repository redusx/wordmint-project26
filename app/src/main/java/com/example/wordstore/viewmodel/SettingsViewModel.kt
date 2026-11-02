package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.domain.repository.AppSettings
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.domain.repository.LanguagePackRepository
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.ThemeMode
import com.example.wordstore.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val packs: List<LanguagePack> = emptyList()
)

/** Shared by Settings and Onboarding, which edit the same preferences. */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val packs: LanguagePackRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(settings.settings, packs.packs, ::SettingsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.settings.value))

    init {
        viewModelScope.launch { packs.refresh() }
    }

    fun setLearningLanguage(code: String) {
        val current = settings.settings.value
        val known = if (code == current.targetLang) current.sourceLang else current.targetLang
        settings.setLanguages(code, known)
    }

    fun setKnownLanguage(code: String) {
        val current = settings.settings.value
        val learning = if (code == current.sourceLang) current.targetLang else current.sourceLang
        settings.setLanguages(learning, code)
    }

    fun setDailyNewGoal(goal: Int) = settings.setDailyNewGoal(goal)

    fun setReminderEnabled(enabled: Boolean) {
        val current = settings.settings.value
        settings.setReminder(enabled, current.reminderHour, current.reminderMinute)
        reminders.sync()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val current = settings.settings.value
        settings.setReminder(current.reminderEnabled, hour, minute)
        reminders.sync()
    }

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    fun downloadPack(code: String, allowCellular: Boolean) = packs.download(code, allowCellular)

    fun isOnUnmeteredNetwork() = packs.isOnUnmeteredNetwork()

    fun completeOnboarding() = settings.setOnboardingDone()
}
