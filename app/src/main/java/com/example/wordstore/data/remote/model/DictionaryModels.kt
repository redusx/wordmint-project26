package com.example.wordstore.data.remote.model

import com.google.gson.annotations.SerializedName

/** Stored as JSON in the `words` table, so field names must stay stable. */
data class MeaningDto(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String,
    @SerializedName("definitions")
    val definitions: List<DefinitionDto>,
    @SerializedName("synonyms")
    val synonyms: List<String>? = null,
    @SerializedName("antonyms")
    val antonyms: List<String>? = null
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
