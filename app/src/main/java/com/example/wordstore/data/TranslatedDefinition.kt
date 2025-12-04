package com.example.wordstore.data

data class TranslatedDefinition(
    val primaryDefinition: String,      // Definition in source language
    val secondaryDefinition: String,    // Definition in target language
    val partOfSpeech: String,
    val examples: List<TranslatedExample> = emptyList()  // Example sentences
)

data class TranslatedExample(
    val primaryExample: String,    // Example in source language
    val secondaryExample: String   // Example in target language
)
