package com.example.wordstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.model.DictionaryStatus
import com.example.wordstore.domain.model.LookupResult
import com.example.wordstore.domain.model.WordLookup
import com.example.wordstore.domain.repository.DictionaryRepository
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.domain.repository.LanguagePackRepository
import com.example.wordstore.domain.repository.SaveWordResult
import com.example.wordstore.domain.repository.SettingsRepository
import com.example.wordstore.domain.repository.WordRepository
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data object PacksRequired : LookupState
    data class Found(val lookup: WordLookup) : LookupState
}

data class DictionaryUiState(
    val query: String = "",
    val learningLang: String = "en",
    val knownLang: String = "tr",
    val isReversed: Boolean = false,
    val lookup: LookupState = LookupState.Idle,
    val translationDraft: String = "",
    val isSaving: Boolean = false,
    val savedWordId: Long? = null,
    /** What is already saved for this word, which takes priority over a fresh machine translation. */
    val savedTranslation: String? = null,
    val missingPacks: List<String> = emptyList(),
    val downloadingPacks: List<String> = emptyList(),
    val packs: List<LanguagePack> = emptyList()
) {
    val fromLang: String get() = if (isReversed) knownLang else learningLang
    val toLang: String get() = if (isReversed) learningLang else knownLang
    val canSearch: Boolean get() = query.isNotBlank() && lookup !is LookupState.Loading
    val canSave: Boolean
        get() = lookup is LookupState.Found && translationDraft.isNotBlank() && savedWordId == null && !isSaving
}

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModel(
    private val dictionary: DictionaryRepository,
    private val words: WordRepository,
    private val settings: SettingsRepository,
    private val packs: LanguagePackRepository,
    private val clock: Clock
) : ViewModel() {

    private data class Input(
        val query: String = "",
        val isReversed: Boolean = false,
        val lookup: LookupState = LookupState.Idle,
        val translationDraft: String = "",
        val isSaving: Boolean = false
    )

    private val input = MutableStateFlow(Input())
    private var searchJob: Job? = null

    private val savedMatch = input.flatMapLatest { current ->
        val found = current.lookup as? LookupState.Found
        if (found == null) {
            flowOf(null)
        } else {
            words.observeMatch(found.lookup.term, found.lookup.sourceLang, found.lookup.targetLang)
        }
    }

    val state: StateFlow<DictionaryUiState> = combine(
        input,
        settings.settings,
        packs.packs,
        savedMatch
    ) { current, appSettings, packList, match ->
        val required = requiredPacks(appSettings.sourceLang, appSettings.targetLang)
        val byCode = packList.associateBy { it.code }
        DictionaryUiState(
            query = current.query,
            learningLang = appSettings.sourceLang,
            knownLang = appSettings.targetLang,
            isReversed = current.isReversed,
            lookup = current.lookup,
            translationDraft = current.translationDraft,
            isSaving = current.isSaving,
            savedWordId = match?.id,
            savedTranslation = match?.translation,
            // Until the pack list has loaded, nothing is known to be missing.
            missingPacks = if (packList.isEmpty()) emptyList() else required.filter { byCode[it]?.isDownloaded != true },
            downloadingPacks = required.filter { byCode[it]?.isDownloading == true },
            packs = packList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DictionaryUiState())

    init {
        refreshPacks()
    }

    fun refreshPacks() {
        viewModelScope.launch { packs.refresh() }
    }

    fun onQueryChange(query: String) = input.update { it.copy(query = query) }

    fun clear() {
        searchJob?.cancel()
        input.update { Input(isReversed = it.isReversed) }
    }

    fun swapDirection() {
        input.update { it.copy(isReversed = !it.isReversed) }
    }

    fun setLearningLanguage(code: String) {
        val current = settings.settings.value
        // Picking the other side's language swaps them, because a pair must stay two languages.
        val known = if (code == current.targetLang) current.sourceLang else current.targetLang
        settings.setLanguages(code, known)
        resetResult()
    }

    fun setKnownLanguage(code: String) {
        val current = settings.settings.value
        val learning = if (code == current.sourceLang) current.targetLang else current.sourceLang
        settings.setLanguages(learning, code)
        resetResult()
    }

    private fun resetResult() {
        searchJob?.cancel()
        input.update { it.copy(lookup = LookupState.Idle, translationDraft = "") }
    }

    fun downloadPack(code: String, allowCellular: Boolean) = packs.download(code, allowCellular)

    fun isOnUnmeteredNetwork() = packs.isOnUnmeteredNetwork()

    fun search() {
        val current = input.value
        val query = current.query.trim()
        if (query.isEmpty()) return

        val appSettings = settings.settings.value
        val from = if (current.isReversed) appSettings.targetLang else appSettings.sourceLang
        searchJob?.cancel()
        input.update { it.copy(lookup = LookupState.Loading, translationDraft = "") }

        searchJob = viewModelScope.launch {
            when (val result = dictionary.lookup(query, from, appSettings.sourceLang, appSettings.targetLang)) {
                LookupResult.PacksRequired -> input.update { it.copy(lookup = LookupState.PacksRequired) }
                is LookupResult.Success -> input.update {
                    it.copy(
                        lookup = LookupState.Found(result.lookup),
                        translationDraft = result.lookup.translation.orEmpty()
                    )
                }
            }
        }
    }

    fun onTranslationChange(text: String) = input.update { it.copy(translationDraft = text) }

    fun save() {
        val current = input.value
        val lookup = (current.lookup as? LookupState.Found)?.lookup ?: return
        if (current.translationDraft.isBlank() || current.isSaving) return
        input.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val now = clock.millis()
            val result: SaveWordResult = words.save(
                Word(
                    term = lookup.term,
                    translation = current.translationDraft,
                    sourceLang = lookup.sourceLang,
                    targetLang = lookup.targetLang,
                    meanings = lookup.meanings,
                    translatedDefinitions = lookup.translatedDefinitions,
                    phonetic = lookup.phonetic,
                    createdAt = now,
                    updatedAt = now
                )
            )
            // The saved state itself arrives through the observed match on this term.
            check(result is SaveWordResult.Saved || result is SaveWordResult.AlreadyExists)
            input.update { it.copy(isSaving = false) }
        }
    }

    companion object {
        /** English is needed too when neither side is English, because definitions come from it. */
        fun requiredPacks(learning: String, known: String): List<String> =
            listOf(learning, known, "en").distinct()
    }
}

val DictionaryStatus.hasDefinitions: Boolean get() = this == DictionaryStatus.FOUND
