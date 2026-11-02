package com.example.wordstore

import android.content.Context
import com.example.wordstore.audio.PronunciationPlayer
import com.example.wordstore.data.local.database.AppDatabase
import com.example.wordstore.data.remote.api.RetrofitInstance
import com.example.wordstore.data.repository.MlKitLanguagePackRepository
import com.example.wordstore.data.repository.RemoteDictionaryRepository
import com.example.wordstore.data.repository.RoomStudyRepository
import com.example.wordstore.data.repository.RoomWordRepository
import com.example.wordstore.data.repository.SharedPrefsSettingsRepository
import com.example.wordstore.domain.repository.DictionaryRepository
import com.example.wordstore.domain.repository.LanguagePackRepository
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.StudyRepository
import com.example.wordstore.domain.repository.WordRepository
import com.example.wordstore.domain.scheduling.Fsrs6Scheduler
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import com.example.wordstore.domain.study.DeviceClock
import com.example.wordstore.reminders.ReminderNotifier
import com.example.wordstore.reminders.ReminderScheduler
import com.example.wordstore.util.TranslationManager
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application-wide object graph. Constructed once, and everything is created on first use. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val clock: Clock = DeviceClock
    val scheduler: SpacedRepetitionScheduler = Fsrs6Scheduler()
    val database: AppDatabase by lazy { AppDatabase.getDatabase(appContext) }
    val settings: SettingsRepository by lazy { SharedPrefsSettingsRepository(appContext) }
    val words: WordRepository by lazy { RoomWordRepository(database.wordDao()) }
    val study: StudyRepository by lazy { RoomStudyRepository(database.studyDao(), scheduler, settings, clock) }
    val dictionary: DictionaryRepository by lazy { RemoteDictionaryRepository(RetrofitInstance.api, TranslationManager) }
    val languagePacks: LanguagePackRepository by lazy { MlKitLanguagePackRepository(appContext, applicationScope) }
    val pronunciation: PronunciationPlayer by lazy { PronunciationPlayer(appContext) }
    val reminderNotifier: ReminderNotifier by lazy { ReminderNotifier(appContext) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext, settings, clock) }
}
