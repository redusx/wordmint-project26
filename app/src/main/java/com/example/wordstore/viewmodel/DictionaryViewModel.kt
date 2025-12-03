package com.example.wordstore.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.DictionaryEntry
import com.example.wordstore.data.DictionaryRepository
import com.example.wordstore.data.NetworkResult
import com.example.wordstore.data.WordSearchResult
import kotlinx.coroutines.launch

class DictionaryViewModel : ViewModel() {

    private val repository = DictionaryRepository()

    private val _searchResult = MutableLiveData<NetworkResult<WordSearchResult>>()
    val searchResult: LiveData<NetworkResult<WordSearchResult>> = _searchResult

    fun searchWord(query: String, sourceLang: String, targetLang: String) {
        viewModelScope.launch {
            repository.searchWord(query, sourceLang, targetLang).collect { result ->
                _searchResult.value = result
            }
        }
    }

    fun resetState() {
        _searchResult.value = null
    }
}
