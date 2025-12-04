package com.example.wordstore.api

import com.example.wordstore.data.DictionaryResponseV1
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApi {
    @GET("api/v1/entries/en/{word}")
    suspend fun getDefinition(@Path("word") word: String): Response<DictionaryResponseV1>
}
