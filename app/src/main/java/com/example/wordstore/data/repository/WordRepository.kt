package com.example.wordstore.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.wordstore.data.local.database.WordDao
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.local.entity.WordWithCard
import com.example.wordstore.domain.repository.SaveWordResult
import com.example.wordstore.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow

class RoomWordRepository(private val wordDao: WordDao) : WordRepository {
    override fun observeWords(): Flow<List<WordWithCard>> = wordDao.observeAll()

    override fun observeWord(id: Long): Flow<WordWithCard?> = wordDao.observe(id)

    override fun observeHistory(wordId: Long): Flow<List<ReviewLogEntity>> = wordDao.observeHistory(wordId)

    override fun observeMatch(term: String, sourceLang: String, targetLang: String): Flow<Word?> =
        wordDao.observeMatch(Word.termKeyOf(term), sourceLang, targetLang)

    override suspend fun save(word: Word): SaveWordResult {
        val normalized = word.copy(term = word.term.trim(), translation = word.translation.trim())
        existing(normalized)?.let { return SaveWordResult.AlreadyExists(it.id) }
        return try {
            SaveWordResult.Saved(wordDao.insertWithCard(normalized))
        } catch (_: SQLiteConstraintException) {
            SaveWordResult.AlreadyExists(existing(normalized)?.id ?: 0L)
        }
    }

    override suspend fun update(word: Word): SaveWordResult {
        val normalized = word.copy(
            term = word.term.trim(),
            translation = word.translation.trim(),
            termKey = Word.termKeyOf(word.term)
        )
        val clash = existing(normalized)
        if (clash != null && clash.id != word.id) return SaveWordResult.AlreadyExists(clash.id)
        wordDao.update(normalized)
        return SaveWordResult.Saved(word.id)
    }

    override suspend fun delete(wordId: Long) = wordDao.delete(wordId)

    private suspend fun existing(word: Word): Word? =
        wordDao.find(word.termKey, word.sourceLang, word.targetLang)
}
