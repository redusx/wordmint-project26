package com.example.wordstore.network

import com.example.wordstore.data.TranslationRequest
import com.example.wordstore.data.TranslationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HuggingFaceApiService {
    @POST(".")
    suspend fun translate(
        @Header("Authorization") authHeader: String,
        @Body request: TranslationRequest
    ): Response<List<TranslationResponse>>
} 