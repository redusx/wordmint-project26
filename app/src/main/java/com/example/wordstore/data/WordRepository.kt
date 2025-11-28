package com.example.wordstore.data

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
