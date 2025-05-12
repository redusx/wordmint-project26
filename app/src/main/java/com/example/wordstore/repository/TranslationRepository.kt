package com.example.wordstore.repository

import com.example.wordstore.data.ResultWrapper
import com.example.wordstore.data.TranslationRequest
import com.example.wordstore.data.TranslationResponse
import com.example.wordstore.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationRepository {
    private val apiService = ApiClient.huggingFaceApiService
    
    suspend fun translateText(text: String): ResultWrapper<List<TranslationResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = ApiClient.getAuthHeader()
                // Use the raw text directly without any prefix
                val request = TranslationRequest(inputs = text)
                val response = apiService.translate(authHeader, request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        ResultWrapper.Success(body)
                    } else {
                        ResultWrapper.Error("Response body is null")
                    }
                } else {
                    ResultWrapper.Error("API call failed with code: ${response.code()}, message: ${response.message()}")
                }
            } catch (e: Exception) {
                ResultWrapper.Error("Network error: ${e.message}", e)
            }
        }
    }
} 