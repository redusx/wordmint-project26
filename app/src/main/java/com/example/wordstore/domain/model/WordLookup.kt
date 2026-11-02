package com.example.wordstore.domain.model

import com.example.wordstore.data.remote.model.MeaningDto

enum class DictionaryStatus {
    FOUND,
    NOT_FOUND,
    OFFLINE,
    SERVICE_ERROR
}

/** What a lookup produced. Definitions and a translation can each be missing independently. */
data class WordLookup(
    val term: String,
    val translation: String?,
    val phonetic: String?,
    val meanings: List<MeaningDto>,
    val translatedDefinitions: List<TranslatedDefinition>,
    val dictionaryStatus: DictionaryStatus,
    val sourceLang: String,
    val targetLang: String
)

sealed interface LookupResult {
    data class Success(val lookup: WordLookup) : LookupResult

    /** The word in the other language was typed, but its pack is needed to find the term. */
    data object PacksRequired : LookupResult
}
