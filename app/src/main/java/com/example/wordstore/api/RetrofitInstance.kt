package com.example.wordstore.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/"
    private const val TRANSLATION_BASE_URL = "https://api.mymemory.translated.net/"

    val api: DictionaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(DICTIONARY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApi::class.java)
    }

    val translationApi: TranslationApi by lazy {
        Retrofit.Builder()
            .baseUrl(TRANSLATION_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranslationApi::class.java)
    }
}
