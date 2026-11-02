package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.local.entity.WordWithCard
import com.example.wordstore.domain.repository.WordRepository
import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.domain.study.isDue
import com.example.wordstore.domain.study.status
import java.text.Collator
import java.time.Clock
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class WordFilter { ALL, NEW, LEARNING, DUE, MASTERED }

enum class WordSort { RECENT, ALPHABETICAL, NEXT_REVIEW, HARDEST }

data class LibraryRow(
    val word: Word,
    val memory: CardMemory,
    val status: LearningStatus,
    val isDue: Boolean,
    val recall: Float
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val nowMillis: Long = 0,
    val query: String = "",
    val filter: WordFilter = WordFilter.ALL,
    val sort: WordSort = WordSort.RECENT,
    val rows: List<LibraryRow> = emptyList(),
    val counts: Map<WordFilter, Int> = emptyMap(),
    val totalWords: Int = 0
)

/** Filtering and sorting, kept pure so they can be tested without a database. */
object LibraryQuery {
    fun matches(row: LibraryRow, filter: WordFilter): Boolean = when (filter) {
        WordFilter.ALL -> true
        WordFilter.NEW -> row.status == LearningStatus.NEW
        WordFilter.LEARNING -> row.status == LearningStatus.LEARNING
        WordFilter.DUE -> row.isDue
        WordFilter.MASTERED -> row.status == LearningStatus.MASTERED
    }

    fun matches(row: LibraryRow, query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return row.word.term.contains(needle, ignoreCase = true) ||
            row.word.translation.contains(needle, ignoreCase = true)
    }

    fun sort(rows: List<LibraryRow>, sort: WordSort, locale: Locale = Locale.getDefault()): List<LibraryRow> = when (sort) {
        WordSort.RECENT -> rows.sortedWith(compareByDescending<LibraryRow> { it.word.createdAt }.thenByDescending { it.word.id })
        WordSort.ALPHABETICAL -> {
            val collator = Collator.getInstance(locale)
            rows.sortedWith { a, b -> collator.compare(a.word.term, b.word.term) }
        }
        // Never-reviewed words have no meaningful date, so they follow everything scheduled.
        WordSort.NEXT_REVIEW -> rows.sortedWith(compareBy({ it.status == LearningStatus.NEW }, { it.memory.dueAt }, { it.word.id }))
        WordSort.HARDEST -> rows.sortedWith(
            compareByDescending<LibraryRow> { it.memory.lapses }
                .thenByDescending { it.memory.difficulty ?: 0.0 }
                .thenBy { it.word.id }
        )
    }
}

class LibraryViewModel(
    words: WordRepository,
    private val scheduler: SpacedRepetitionScheduler,
    clock: Clock
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(WordFilter.ALL)
    private val sort = MutableStateFlow(WordSort.RECENT)

    val state: StateFlow<LibraryUiState> = combine(
        words.observeWords(),
        query,
        filter,
        sort,
        clock.ticks()
    ) { all, currentQuery, currentFilter, currentSort, now ->
        val rows = all.mapNotNull { toRow(it, now) }
        val searched = rows.filter { LibraryQuery.matches(it, currentQuery) }
        LibraryUiState(
            isLoading = false,
            nowMillis = now,
            query = currentQuery,
            filter = currentFilter,
            sort = currentSort,
            rows = LibraryQuery.sort(searched.filter { LibraryQuery.matches(it, currentFilter) }, currentSort),
            counts = WordFilter.entries.associateWith { option -> searched.count { LibraryQuery.matches(it, option) } },
            totalWords = rows.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private fun toRow(item: WordWithCard, now: Long): LibraryRow? {
        val memory = item.card?.toMemory() ?: return null
        return LibraryRow(
            word = item.word,
            memory = memory,
            status = memory.status(),
            isDue = memory.isDue(now),
            recall = scheduler.retrievability(memory, now).toFloat()
        )
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: WordFilter) {
        filter.value = value
    }

    fun onSortChange(value: WordSort) {
        sort.value = value
    }
}
