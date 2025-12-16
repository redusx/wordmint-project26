package com.example.wordstore

import android.app.Application
import com.example.wordstore.data.local.database.AppDatabase
import com.example.wordstore.util.TranslationManager
import com.example.wordstore.data.repository.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WordStoreApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { WordRepository(database.wordDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize TranslationManager with context for notifications
        // Note: Language models are downloaded on-demand by user action
        TranslationManager.initialize(this)
    }
}
