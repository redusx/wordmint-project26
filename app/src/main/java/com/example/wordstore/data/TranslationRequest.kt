package com.example.wordstore.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TranslationRequest(
    @Json(name = "inputs")
    val inputs: String,
    
    @Json(name = "parameters")
    val parameters: TranslationParameters? = null
)

@JsonClass(generateAdapter = true)
data class TranslationParameters(
    @Json(name = "tgt_lang")
    val targetLanguage: String
) 