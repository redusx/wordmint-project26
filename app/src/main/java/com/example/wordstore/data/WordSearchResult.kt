package com.example.wordstore.data

data class WordSearchResult(
    val originalQuery: String,
    val englishWord: String,
    val turkishTranslation: String?,
    val definitions: List<DictionaryEntry>?,
    val translatedDefinitions: List<TranslatedDefinition>? = null
)
