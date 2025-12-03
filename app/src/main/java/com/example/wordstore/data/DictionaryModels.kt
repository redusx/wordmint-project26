package com.example.wordstore.data

import com.google.gson.annotations.SerializedName

data class DictionaryEntry(
    @SerializedName("word")
    val word: String,
    @SerializedName("phonetic")
    val phonetic: String? = null,
    @SerializedName("phonetics")
    val phonetics: List<PhoneticDto>? = null,
    @SerializedName("meanings")
    val meanings: List<MeaningDto>? = null,
    @SerializedName("sourceUrls")
    val sourceUrls: List<String>? = null
)

data class PhoneticDto(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("audio")
    val audio: String? = null
)

data class MeaningDto(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String,
    @SerializedName("definitions")
    val definitions: List<DefinitionDto>
)

data class DefinitionDto(
    @SerializedName("definition")
    val definition: String,
    @SerializedName("example")
    val example: String? = null,
    @SerializedName("synonyms")
    val synonyms: List<String>? = null,
    @SerializedName("antonyms")
    val antonyms: List<String>? = null
)
