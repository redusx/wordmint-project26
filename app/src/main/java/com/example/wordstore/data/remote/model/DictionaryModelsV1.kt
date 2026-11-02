package com.example.wordstore.data.remote.model

import com.google.gson.annotations.SerializedName

// V1 API Response Models
data class DictionaryResponseV1(
    @SerializedName("word")
    val word: String?,
    @SerializedName("entries")
    val entries: List<EntryV1>?
)

data class EntryV1(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String?,
    @SerializedName("pronunciations")
    val pronunciations: List<PronunciationV1>?,
    @SerializedName("senses")
    val senses: List<SenseV1>?,
    @SerializedName("synonyms")
    val synonyms: List<String>?,
    @SerializedName("antonyms")
    val antonyms: List<String>?
)

data class SenseV1(
    @SerializedName("definition")
    val definition: String?,
    @SerializedName("examples")
    val examples: List<String>?,
    @SerializedName("synonyms")
    val synonyms: List<String>?,
    @SerializedName("antonyms")
    val antonyms: List<String>?
)

data class PronunciationV1(
    @SerializedName("type")
    val type: String?,
    @SerializedName("text")
    val text: String?
)
