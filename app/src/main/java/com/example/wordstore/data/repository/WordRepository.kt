package com.example.wordstore.data.repository

import com.example.wordstore.data.local.database.WordDao
import com.example.wordstore.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()

    suspend fun insert(word: Word) {
        wordDao.insertWord(word)
    }

    suspend fun delete(word: Word) {
        wordDao.deleteWord(word)
    }
}
