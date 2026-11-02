package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.local.entity.ReviewLogEntity
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.repository.SaveWordResult
import com.example.wordstore.domain.repository.WordRepository
import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.SpacedRepetitionScheduler
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.domain.study.isDue
import com.example.wordstore.domain.study.status
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WordDetail(
    val word: Word,
    val memory: CardMemory,
    val status: LearningStatus,
    val isDue: Boolean,
    val recall: Float,
    val history: List<ReviewLogEntity>
)

data class WordDetailUiState(
    val isLoading: Boolean = true,
    val nowMillis: Long = 0,
    val detail: WordDetail? = null
)

data class WordEdits(val term: String, val translation: String, val phonetic: String, val hint: String)

enum class EditResult { IDLE, SAVED, DUPLICATE }

class WordDetailViewModel(
    private val wordId: Long,
    private val words: WordRepository,
    private val scheduler: SpacedRepetitionScheduler,
    private val clock: Clock
) : ViewModel() {

    private val mutableEditResult = MutableStateFlow(EditResult.IDLE)
    val editResult: StateFlow<EditResult> = mutableEditResult.asStateFlow()

    val state: StateFlow<WordDetailUiState> = combine(
        words.observeWord(wordId),
        words.observeHistory(wordId),
        clock.ticks()
    ) { item, history, now ->
        val memory = item?.card?.toMemory()
        WordDetailUiState(
            isLoading = false,
            nowMillis = now,
            detail = if (item == null || memory == null) {
                null
            } else {
                WordDetail(
                    word = item.word,
                    memory = memory,
                    status = memory.status(),
                    isDue = memory.isDue(now),
                    recall = scheduler.retrievability(memory, now).toFloat(),
                    history = history
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordDetailUiState())

    fun save(edits: WordEdits) {
        val word = state.value.detail?.word ?: return
        if (edits.term.isBlank() || edits.translation.isBlank()) return
        viewModelScope.launch {
            val result = words.update(
                word.copy(
                    term = edits.term,
                    translation = edits.translation,
                    phonetic = edits.phonetic.trim().ifEmpty { null },
                    hint = edits.hint.trim().ifEmpty { null },
                    updatedAt = clock.millis()
                )
            )
            mutableEditResult.value = if (result is SaveWordResult.AlreadyExists) EditResult.DUPLICATE else EditResult.SAVED
        }
    }

    fun clearEditResult() {
        mutableEditResult.value = EditResult.IDLE
    }

    fun delete() {
        viewModelScope.launch { words.delete(wordId) }
    }
}
