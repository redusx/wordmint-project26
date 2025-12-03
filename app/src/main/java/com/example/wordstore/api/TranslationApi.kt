package com.example.wordstore.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationApi {
    @GET("get")
    suspend fun getTranslation(
        @Query("q") text: String,
        @Query("langpair") langPair: String
    ): Response<TranslationResponse>
}

data class TranslationResponse(
    val responseData: ResponseData,
    val responseStatus: Int,
    val matches: List<MatchDto>? = null
)

data class ResponseData(
    val translatedText: String,
    val match: Double
)

data class MatchDto(
    val id: String,
    val segment: String,
    val translation: String,
    val source: String,
    val target: String,
    val match: Double
)
