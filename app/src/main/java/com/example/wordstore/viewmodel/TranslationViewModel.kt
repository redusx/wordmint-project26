package com.example.wordstore.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.ResultWrapper
import com.example.wordstore.data.TranslationResponse
import com.example.wordstore.repository.TranslationRepository
import kotlinx.coroutines.launch

class TranslationViewModel : ViewModel() {
    private val repository = TranslationRepository()
    
    private val _translationResult = MutableLiveData<ResultWrapper<List<TranslationResponse>>>()
    val translationResult: LiveData<ResultWrapper<List<TranslationResponse>>> = _translationResult
    
    fun translateText(text: String) {
        _translationResult.value = ResultWrapper.Loading
        
        viewModelScope.launch {
            val result = repository.translateText(text)
            _translationResult.postValue(result)
        }
    }
    
    // Helper function to get the translated text from the response
    fun getTranslatedText(): String {
        val currentResult = _translationResult.value
        return if (currentResult is ResultWrapper.Success) {
            currentResult.data.firstOrNull()?.translationText ?: ""
        } else {
            ""
        }
    }
} 