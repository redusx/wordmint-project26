package com.example.wordstore.domain.model

data class TranslatedDefinition(
    val primaryDefinition: String,      // Definition in the term's language
    val secondaryDefinition: String,    // Definition in the translation's language
    val partOfSpeech: String,
    val examples: List<TranslatedExample> = emptyList()  // Example sentences
)

data class TranslatedExample(
    val primaryExample: String,    // Example in the term's language
    val secondaryExample: String   // Example in the translation's language
)
