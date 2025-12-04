package com.example.wordstore.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.DictionaryRepository
import com.example.wordstore.data.NetworkResult
import com.example.wordstore.data.WordSearchResult
import kotlinx.coroutines.launch

class DictionaryViewModel : ViewModel() {

    private val repository = DictionaryRepository()

    private val _searchResult = MutableLiveData<NetworkResult<WordSearchResult>>()
    val searchResult: LiveData<NetworkResult<WordSearchResult>> = _searchResult

    private val _languageStatus = MutableLiveData<List<LanguageModelStatus>>()
    val languageStatus: LiveData<List<LanguageModelStatus>> = _languageStatus
    
    // Cache for language status to avoid repeated expensive checks
    private var cachedLanguageStatus: List<LanguageModelStatus>? = null
    private var lastStatusCheckTime: Long = 0
    private val STATUS_CACHE_DURATION = 30_000L // 30 seconds

    // Download state
    private val _downloadState = MutableLiveData<DownloadState>()
    val downloadState: LiveData<DownloadState> = _downloadState
    
    // Download progress
    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> = _downloadProgress

    init {
        // Set up progress callback
        com.example.wordstore.data.TranslationManager.onProgressUpdate = { langCode, progress ->
            _downloadProgress.postValue(progress)
        }
    }

    fun checkLanguageStatus(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        
        // Use cache if available and fresh
        if (!forceRefresh && cachedLanguageStatus != null && 
            (now - lastStatusCheckTime) < STATUS_CACHE_DURATION) {
            _languageStatus.value = cachedLanguageStatus
            return
        }
        
        viewModelScope.launch {
            try {
                val languages = com.example.wordstore.data.TranslationManager.getAvailableLanguages()
                
                // Only check top languages initially for performance
                val topLanguages = listOf("en", "tr", "de", "fr", "es", "it", "ru", "ar", "ja", "zh")
                val languagesToCheck = languages.filter { it in topLanguages }
                
                val statusList = languagesToCheck.map { code ->
                    LanguageModelStatus(
                        code = code,
                        isDownloaded = com.example.wordstore.data.TranslationManager.isModelDownloaded(code)
                    )
                }
                
                cachedLanguageStatus = statusList
                lastStatusCheckTime = now
                _languageStatus.value = statusList
            } catch (e: Exception) {
                e.printStackTrace()
                _languageStatus.value = emptyList()
            }
        }
    }

    fun downloadLanguage(code: String, allowCellular: Boolean = false) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(code)
            _downloadProgress.value = 0
            
            val error = com.example.wordstore.data.TranslationManager.downloadModel(
                code, 
                requireWifi = !allowCellular,
                showNotification = true
            )
            
            if (error == null) {
                _downloadState.value = DownloadState.Success(code)
                _downloadProgress.value = 100
                checkLanguageStatus(forceRefresh = true) // Refresh status
            } else {
                _downloadState.value = DownloadState.Error(code, error)
                _downloadProgress.value = 0
            }
        }
    }

    fun deleteLanguage(code: String) {
        viewModelScope.launch {
            val error = com.example.wordstore.data.TranslationManager.deleteModel(code)
            
            if (error == null) {
                checkLanguageStatus(forceRefresh = true)
            } else {
                _downloadState.value = DownloadState.Error(code, error)
            }
        }
    }

    fun searchWord(query: String, sourceLang: String, targetLang: String) {
        viewModelScope.launch {
            repository.searchWord(query, sourceLang, targetLang).collect { result ->
                _searchResult.value = result
            }
        }
    }

    fun resetState() {
        _searchResult.value = null
        _downloadState.value = null
        _downloadProgress.value = 0
    }
}

data class LanguageModelStatus(
    val code: String,
    val isDownloaded: Boolean
)

sealed class DownloadState {
    data class Downloading(val langCode: String) : DownloadState()
    data class Success(val langCode: String) : DownloadState()
    data class Error(val langCode: String, val message: String) : DownloadState()
}
