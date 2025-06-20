package com.example.wordstore.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.FetchState
import kotlinx.coroutines.launch

class TranslationViewModel : ViewModel() {

    private val _translationState = MutableLiveData<FetchState>()
    val translationState: LiveData<FetchState> = _translationState

    fun getTranslation(word: String) {
        if (word.isBlank()) {
            _translationState.value = FetchState.Error("Please enter a word.")
            return
        }
        _translationState.value = FetchState.Loading
        viewModelScope.launch {
            val result = fetchCambridgeTranslation(word)
            _translationState.postValue(result)
        }
    }
} 