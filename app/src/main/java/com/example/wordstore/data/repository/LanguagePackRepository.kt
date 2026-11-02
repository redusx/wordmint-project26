package com.example.wordstore.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.domain.repository.LanguagePackRepository
import com.example.wordstore.domain.repository.PackEvent
import com.example.wordstore.util.PackDownloadResult
import com.example.wordstore.util.TranslationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Application-scoped, so a download keeps going when the user leaves the screen that started it. */
class MlKitLanguagePackRepository(
    private val context: Context,
    private val scope: CoroutineScope
) : LanguagePackRepository {
    private val downloading = MutableStateFlow<Set<String>>(emptySet())
    private val state = MutableStateFlow<List<LanguagePack>>(emptyList())
    private val eventFlow = MutableSharedFlow<PackEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val packs: StateFlow<List<LanguagePack>> = state.asStateFlow()
    override val events: Flow<PackEvent> = eventFlow

    override suspend fun refresh() {
        val downloaded = TranslationManager.downloadedLanguages()
        val active = downloading.value
        state.value = TranslationManager.availableLanguages().map { code ->
            LanguagePack(code, code in downloaded, code in active)
        }
    }

    override fun download(code: String, allowCellular: Boolean) {
        if (code in downloading.value) return
        downloading.update { it + code }
        publishDownloading()
        scope.launch {
            val result = TranslationManager.downloadModel(code, requireWifi = !allowCellular)
            downloading.update { it - code }
            refresh()
            when (result) {
                PackDownloadResult.Success -> eventFlow.emit(PackEvent.Downloaded(code))
                is PackDownloadResult.Failure -> eventFlow.emit(PackEvent.Failed(code, result.reason))
            }
        }
    }

    private fun publishDownloading() {
        val active = downloading.value
        state.update { list -> list.map { it.copy(isDownloading = it.code in active) } }
    }

    override fun isOnUnmeteredNetwork(): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork ?: return false) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
