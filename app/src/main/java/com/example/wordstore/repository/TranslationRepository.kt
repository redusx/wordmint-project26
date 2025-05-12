package com.example.wordstore.repository

import com.example.wordstore.data.ResultWrapper
import com.example.wordstore.data.TranslationParameters
import com.example.wordstore.data.TranslationRequest
import com.example.wordstore.data.TranslationResponse
import com.example.wordstore.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationRepository {
    private val apiService = ApiClient.huggingFaceApiService
    
    // Target language code for Turkish
    private val TARGET_LANGUAGE = "tr"
    
    suspend fun translateText(text: String): ResultWrapper<List<TranslationResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = ApiClient.getAuthHeader()
                
                // Add the language prefix to the input text (required by Helsinki-NLP models)
                val prefixedText = ">>$TARGET_LANGUAGE<< $text"
                
                // Create request with both prefixed text and parameters
                val parameters = TranslationParameters(targetLanguage = TARGET_LANGUAGE)
                val request = TranslationRequest(
                    inputs = prefixedText,
                    parameters = parameters
                )
                
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