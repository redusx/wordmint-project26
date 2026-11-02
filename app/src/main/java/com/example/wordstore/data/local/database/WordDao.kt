package com.example.wordstore.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.wordstore.data.local.entity.CardSchedulingEntity
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.local.entity.WordWithCard
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WordDao {
    @Transaction
    @Query("SELECT * FROM words ORDER BY createdAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<WordWithCard>>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    abstract fun observe(id: Long): Flow<WordWithCard?>

    @Query("SELECT * FROM words WHERE termKey = :termKey AND sourceLang = :sourceLang AND targetLang = :targetLang LIMIT 1")
    abstract suspend fun find(termKey: String, sourceLang: String, targetLang: String): Word?

    @Query("SELECT * FROM words WHERE termKey = :termKey AND sourceLang = :sourceLang AND targetLang = :targetLang LIMIT 1")
    abstract fun observeMatch(termKey: String, sourceLang: String, targetLang: String): Flow<Word?>

    @Query("SELECT * FROM review_logs WHERE wordId = :wordId ORDER BY reviewedAt DESC")
    abstract fun observeHistory(wordId: Long): Flow<List<ReviewLogEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertWord(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertCard(card: CardSchedulingEntity)

    @Update
    abstract suspend fun update(word: Word)

    @Query("DELETE FROM words WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** Saving a word and making it eligible for learning is one atomic step. */
    @Transaction
    open suspend fun insertWithCard(word: Word): Long {
        val id = insertWord(word)
        insertCard(CardSchedulingEntity(wordId = id, dueAt = word.createdAt))
        return id
    }
}
