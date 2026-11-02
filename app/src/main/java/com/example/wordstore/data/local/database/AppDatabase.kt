package com.example.wordstore.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wordstore.data.local.entity.CardSchedulingEntity
import com.example.wordstore.data.local.entity.Converters
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word

@Database(
    entities = [Word::class, CardSchedulingEntity::class, ReviewLogEntity::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun studyDao(): StudyDao

    companion object {
        const val NAME = "word_database"

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                    .addMigrations(Migration6To7())
                    // Versions before 6 have no recorded schema to migrate from. From 6 on, no path is destructive.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
