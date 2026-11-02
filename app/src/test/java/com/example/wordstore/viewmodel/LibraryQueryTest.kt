package com.example.wordstore.viewmodel

import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.fakes.word
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQueryTest {
    private fun row(
        id: Long,
        term: String,
        status: LearningStatus = LearningStatus.LEARNING,
        isDue: Boolean = false,
        dueAt: Long = 0,
        lapses: Int = 0,
        difficulty: Double? = null,
        createdAt: Long = id,
        translation: String = "t$id"
    ) = LibraryRow(
        word = word(id, term, createdAt, translation),
        memory = CardMemory(cardId = id, state = CardState.REVIEW, dueAt = dueAt, lapses = lapses, difficulty = difficulty),
        status = status,
        isDue = isDue,
        recall = 0f
    )

    @Test
    fun `filters match status and due`() {
        val new = row(1, "a", LearningStatus.NEW)
        val mastered = row(2, "b", LearningStatus.MASTERED, isDue = true)
        assertTrue(LibraryQuery.matches(new, WordFilter.ALL))
        assertTrue(LibraryQuery.matches(new, WordFilter.NEW))
        assertFalse(LibraryQuery.matches(new, WordFilter.DUE))
        assertTrue(LibraryQuery.matches(mastered, WordFilter.DUE))
        assertTrue(LibraryQuery.matches(mastered, WordFilter.MASTERED))
        assertFalse(LibraryQuery.matches(mastered, WordFilter.LEARNING))
    }

    @Test
    fun `search covers term and translation without case`() {
        val row = row(1, "Resilient", translation = "dayanıklı")
        assertTrue(LibraryQuery.matches(row, "resil"))
        assertTrue(LibraryQuery.matches(row, "DAYAN"))
        assertTrue(LibraryQuery.matches(row, "  "))
        assertFalse(LibraryQuery.matches(row, "book"))
    }

    @Test
    fun `sorts by recent, alphabet, next review and difficulty`() {
        val a = row(1, "banana", dueAt = 30, lapses = 0, difficulty = 9.0, createdAt = 10)
        val b = row(2, "Apple", dueAt = 10, lapses = 3, difficulty = 2.0, createdAt = 30)
        val c = row(3, "çay", status = LearningStatus.NEW, dueAt = 1, createdAt = 20)

        fun ids(sort: WordSort) = LibraryQuery.sort(listOf(a, b, c), sort, Locale("tr")).map { it.word.id }
        assertEquals(listOf(2L, 3L, 1L), ids(WordSort.RECENT))
        assertEquals(listOf(2L, 1L, 3L), ids(WordSort.ALPHABETICAL))
        assertEquals(listOf(2L, 1L, 3L), ids(WordSort.NEXT_REVIEW))
        assertEquals(listOf(2L, 1L, 3L), ids(WordSort.HARDEST))
    }
}
