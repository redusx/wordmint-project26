package com.example.wordstore.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordstore.data.repository.DictionaryRepository
import com.example.wordstore.domain.model.NetworkResult
import com.example.wordstore.domain.model.WordSearchResult
import com.example.wordstore.util.TranslationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DictionaryViewModel : ViewModel() {

    private val repository = DictionaryRepository()

    private val _searchResult = MutableLiveData<NetworkResult<WordSearchResult>>()
    val searchResult: LiveData<NetworkResult<WordSearchResult>> = _searchResult

    private val _languageStatus = MutableLiveData<List<LanguageModelStatus>>()
    val languageStatus: LiveData<List<LanguageModelStatus>> = _languageStatus
    
    // Cache for language status
    private var cachedLanguageStatus: List<LanguageModelStatus>? = null
    private var lastStatusCheckTime: Long = 0
    private val STATUS_CACHE_DURATION = 30_000L // 30 seconds

    // Parallel download support - Map of language code to download state
    private val _downloadingLanguages = MutableLiveData<Set<String>>(emptySet())
    val downloadingLanguages: LiveData<Set<String>> = _downloadingLanguages
    
    // Download events for snackbar notifications
    private val _downloadEvent = MutableLiveData<DownloadEvent?>()
    val downloadEvent: LiveData<DownloadEvent?> = _downloadEvent
    
    // Map to track download jobs per language
    private val downloadJobs = mutableMapOf<String, Job>()
    
    // Network monitoring
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        private const val TAG = "DictionaryViewModel"
    }

    init {
        // Set up progress callback
        TranslationManager.onProgressUpdate = { langCode, progress ->
            // Not used anymore since we're showing indeterminate progress
        }
    }

    fun setupNetworkMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                // Network lost, cancel all ongoing downloads
                val activeDownloads = _downloadingLanguages.value ?: emptySet()
                if (activeDownloads.isNotEmpty()) {
                    activeDownloads.forEach { langCode ->
                        downloadJobs[langCode]?.cancel()
                    }
                    _downloadingLanguages.postValue(emptySet())
                    _downloadEvent.postValue(
                        DownloadEvent.Error("all", "Bağlantı kesildi. İndirmeler iptal edildi.")
                    )
                }
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback?.let {
            connectivityManager.registerNetworkCallback(request, it)
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
                val languages = TranslationManager.getAvailableLanguages()
                
                // Only check top languages initially for performance
                val topLanguages = listOf("en", "tr", "de", "fr", "es", "it", "ru", "ar", "ja", "zh")
                val languagesToCheck = languages.filter { it in topLanguages }
                
                val statusList = languagesToCheck.map { code ->
                    LanguageModelStatus(
                        code = code,
                        isDownloaded = TranslationManager.isModelDownloaded(code)
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

    /**
     * Check if a specific language is currently being downloaded
     */
    fun isDownloading(langCode: String): Boolean {
        return _downloadingLanguages.value?.contains(langCode) == true
    }

    /**
     * Download a language model - supports parallel downloads
     */
    fun downloadLanguage(code: String, allowCellular: Boolean = false) {
        // If already downloading this language, ignore
        if (isDownloading(code)) {
            Log.d(TAG, "Already downloading $code, ignoring duplicate request")
            return
        }
        
        Log.d(TAG, "Starting download for: $code")
        
        // Add to downloading set
        val currentDownloading = _downloadingLanguages.value?.toMutableSet() ?: mutableSetOf()
        currentDownloading.add(code)
        _downloadingLanguages.value = currentDownloading
        
        // Start download job
        val job = viewModelScope.launch {
            try {
                Log.d(TAG, "Download job started for: $code")
                
                val error = TranslationManager.downloadModel(
                    code, 
                    requireWifi = !allowCellular,
                    showNotification = true
                )
                
                if (error == null) {
                    Log.d(TAG, "Download successful for: $code")
                    _downloadEvent.value = DownloadEvent.Success(code)
                    // Clear cache to force refresh
                    cachedLanguageStatus = null
                    checkLanguageStatus(forceRefresh = true)
                } else {
                    Log.e(TAG, "Download error for $code: $error")
                    _downloadEvent.value = DownloadEvent.Error(code, error)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Download cancelled for $code")
                // Don't report cancellation as error
            } catch (e: Exception) {
                Log.e(TAG, "Download exception for $code", e)
                _downloadEvent.value = DownloadEvent.Error(
                    code, 
                    e.message ?: "İndirme başarısız"
                )
            } finally {
                // Remove from downloading set
                removeFromDownloading(code)
                downloadJobs.remove(code)
            }
        }
        
        downloadJobs[code] = job
    }

    /**
     * Cancel a specific download
     */
    fun cancelDownload(code: String) {
        downloadJobs[code]?.cancel()
        removeFromDownloading(code)
        downloadJobs.remove(code)
    }

    private fun removeFromDownloading(code: String) {
        val currentDownloading = _downloadingLanguages.value?.toMutableSet() ?: mutableSetOf()
        currentDownloading.remove(code)
        _downloadingLanguages.postValue(currentDownloading)
    }

    fun deleteLanguage(code: String) {
        viewModelScope.launch {
            val error = TranslationManager.deleteModel(code)
            
            if (error == null) {
                cachedLanguageStatus = null
                checkLanguageStatus(forceRefresh = true)
            } else {
                _downloadEvent.value = DownloadEvent.Error(code, error)
            }
        }
    }

    fun clearDownloadEvent() {
        _downloadEvent.value = null
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
        _downloadEvent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        networkCallback?.let {
            // Note: Can't unregister without ConnectivityManager reference
        }
    }
}

data class LanguageModelStatus(
    val code: String,
    val isDownloaded: Boolean
)

sealed class DownloadEvent {
    data class Success(val langCode: String) : DownloadEvent()
    data class Error(val langCode: String, val message: String) : DownloadEvent()
}
