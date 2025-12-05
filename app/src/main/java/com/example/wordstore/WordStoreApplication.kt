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
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize TranslationManager with context for notifications
        TranslationManager.initialize(this)
        
        // Download EN/TR models in background at startup
        // This doesn't block app startup
        applicationScope.launch {
            // Try to download with WiFi requirement first
            // Will show notification if download starts
            TranslationManager.downloadModels(requireWifi = true, showNotification = true)
        }
    }
}
