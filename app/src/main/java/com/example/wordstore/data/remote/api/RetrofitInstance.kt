package com.example.wordstore.data.remote.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val DICTIONARY_BASE_URL = "https://freedictionaryapi.com/"


    val api: DictionaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(DICTIONARY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApi::class.java)
    }


}
