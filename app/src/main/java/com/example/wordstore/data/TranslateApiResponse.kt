package com.example.wordstore.data

data class TranslationResult(
    val word: String,
    val entries: List<WordEntry>
)

data class WordEntry(
    val partOfSpeech: String, // Kelime Türü (noun, verb etc.)
    val definitions: List<Definition>
)

data class Definition(
    val turkishTranslations: List<String>,
    val examples: List<ExamplePair> = emptyList() // Örnek cümleler (İngilizce-Türkçe)
)

data class ExamplePair(
    val english: String,
    val turkish: String
)

// Hata durumları ve yüklenme durumu için bir sealed class (isteğe bağlı ama iyi pratik)
sealed class FetchState {
    object Loading : FetchState()
    data class Success(val result: TranslationResult) : FetchState()
    data class Error(val message: String) : FetchState()
    object WordNotFound : FetchState() // Kelime bulunamadı durumu
}