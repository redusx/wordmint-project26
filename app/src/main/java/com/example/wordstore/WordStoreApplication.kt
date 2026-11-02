package com.example.wordstore

import android.app.Application
import com.example.wordstore.util.TranslationManager

class WordStoreApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        TranslationManager.initialize(this)
        container.reminderNotifier.createChannel()
        // Language packs are only ever downloaded by a user action.
        container.reminderScheduler.ensureScheduled()
    }
}
