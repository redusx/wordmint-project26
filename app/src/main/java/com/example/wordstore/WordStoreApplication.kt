package com.example.wordstore

import android.app.Application
import com.example.wordstore.data.AppDatabase
import com.example.wordstore.data.WordRepository

class WordStoreApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { WordRepository(database.wordDao()) }
}
