package com.example.wordstore.domain.model

import com.example.wordstore.data.remote.model.DictionaryEntry

data class WordSearchResult(
    val originalQuery: String,
    val englishWord: String,
    val turkishTranslation: String?,
    val definitions: List<DictionaryEntry>?,
    val translatedDefinitions: List<TranslatedDefinition>? = null,
    val errorMessage: String? = null
)
