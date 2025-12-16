package com.example.wordstore.util

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

object TranslationManager {

    private var englishToTurkishTranslator: Translator? = null
    private var turkishToEnglishTranslator: Translator? = null
    
    // Cache for dynamic translators
    private val translatorCache = mutableMapOf<String, Translator>()
    
    // Track initialization status
    private var isInitialized = false
    private var lastError: String? = null
    
    // Progress callback
    var onProgressUpdate: ((String, Int) -> Unit)? = null
    var notificationManager: ModelDownloadNotificationManager? = null

    /**
     * Initialize translators (doesn't download models yet)
     */
    fun initialize(context: Context? = null) {
        if (isInitialized) return
        
        context?.let {
            notificationManager = ModelDownloadNotificationManager(it)
        }
        
        val optionsEnTr = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TURKISH)
            .build()
        englishToTurkishTranslator = Translation.getClient(optionsEnTr)

        val optionsTrEn = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.TURKISH)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        turkishToEnglishTranslator = Translation.getClient(optionsTrEn)
        
        isInitialized = true
    }

    /**
     * Get or create translator for any language pair
     */
    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "$sourceLang-$targetLang"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }

    /**
     * Generic translation for any language pair
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        return try {
            if (!isInitialized) initialize()
            
            // Check if models are downloaded
            val sourceReady = isModelDownloaded(sourceLang)
            val targetReady = isModelDownloaded(targetLang)
            
            if (!sourceReady || !targetReady) {
                return Result.failure(Exception(
                    "Dil modelleri hazır değil. Lütfen ${sourceLang.uppercase()} ve ${targetLang.uppercase()} modellerini indirin."
                ))
            }
            
            val translator = getTranslator(sourceLang, targetLang)
            val result = translator.translate(text).await()
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Çeviri yapılamadı."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = if (e.message?.contains("model", ignoreCase = true) == true) {
                "Dil modeli hazır değil. Lütfen ${sourceLang.uppercase()}→${targetLang.uppercase()} modellerini indirin."
            } else {
                "Çeviri hatası: ${e.message ?: "Bilinmeyen hata"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    /**
     * Check if models are ready for translation
     */
    suspend fun areModelsReady(): Boolean {
        return try {
            val enReady = isModelDownloaded(TranslateLanguage.ENGLISH)
            val trReady = isModelDownloaded(TranslateLanguage.TURKISH)
            enReady && trReady
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Download models with optional WiFi requirement and progress tracking
     * @param requireWifi If true, only download on WiFi. If false, allow cellular.
     * @param showNotification If true, show notification bar updates
     * @return Error message if failed, null if successful
     */
    suspend fun downloadModels(requireWifi: Boolean = false, showNotification: Boolean = true): String? {
        android.util.Log.d("TranslationManager", "downloadModels started, requireWifi: $requireWifi")
        
        return try {
            if (!isInitialized) {
                android.util.Log.e("TranslationManager", "downloadModels failed: not initialized")
                return "TranslationManager başlatılmamış."
            }
            
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            val enModel = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build()
            val trModel = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(TranslateLanguage.TURKISH).build()
            
            val conditions = DownloadConditions.Builder()
                .apply { if (requireWifi) requireWifi() }
                .build()

            // Check if already downloaded
            val enDownloaded = try { modelManager.isModelDownloaded(enModel).await() } catch (e: Exception) { false }
            val trDownloaded = try { modelManager.isModelDownloaded(trModel).await() } catch (e: Exception) { false }
            
            android.util.Log.d("TranslationManager", "EN downloaded: $enDownloaded, TR downloaded: $trDownloaded")
            
            if (enDownloaded && trDownloaded) {
                android.util.Log.d("TranslationManager", "Both models already downloaded")
                return null // Already done
            }

            // Start download notification
            if (showNotification) {
                notificationManager?.showDownloadStarted("EN/TR")
            }
            
            // Download English if needed
            if (!enDownloaded) {
                android.util.Log.d("TranslationManager", "Downloading EN model...")
                onProgressUpdate?.invoke("en", 0)
                withTimeout(90_000L) {
                    modelManager.download(enModel, conditions).await()
                }
                android.util.Log.d("TranslationManager", "EN model downloaded")
            }
            onProgressUpdate?.invoke("en", 100)
            
            // Download Turkish if needed
            if (!trDownloaded) {
                android.util.Log.d("TranslationManager", "Downloading TR model...")
                onProgressUpdate?.invoke("tr", 0)
                withTimeout(90_000L) {
                    modelManager.download(trModel, conditions).await()
                }
                android.util.Log.d("TranslationManager", "TR model downloaded")
            }
            onProgressUpdate?.invoke("tr", 100)
            
            // Complete notification
            if (showNotification) {
                notificationManager?.showDownloadComplete("EN/TR")
            }
            
            android.util.Log.d("TranslationManager", "downloadModels completed successfully")
            lastError = null
            null // Success
        } catch (e: TimeoutCancellationException) {
            android.util.Log.e("TranslationManager", "downloadModels timeout", e)
            val errorMsg = "İndirme zaman aşımına uğradı. Lütfen tekrar deneyin."
            lastError = errorMsg
            if (showNotification) {
                notificationManager?.showDownloadError("EN/TR", errorMsg)
            }
            errorMsg
        } catch (e: Exception) {
            android.util.Log.e("TranslationManager", "downloadModels failed", e)
            val errorMsg = when {
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "İnternet bağlantısı gerekli. Lütfen bağlantınızı kontrol edin."
                e.message?.contains("storage", ignoreCase = true) == true ->
                    "Yeterli depolama alanı yok."
                e.message?.contains("wifi", ignoreCase = true) == true ->
                    "WiFi bağlantısı gerekli."
                else ->
                    "Model indirilemedi: ${e.message ?: "Bilinmeyen hata"}"
            }
            lastError = errorMsg
            e.printStackTrace()
            
            if (showNotification) {
                notificationManager?.showDownloadError("EN/TR", errorMsg)
            }
            
            errorMsg
        }
    }

    suspend fun translateEnToTr(text: String): Result<String> {
        return translate(text, TranslateLanguage.ENGLISH, TranslateLanguage.TURKISH)
    }

    suspend fun translateTrToEn(text: String): Result<String> {
        return translate(text, TranslateLanguage.TURKISH, TranslateLanguage.ENGLISH)
    }

    fun getLastError(): String? = lastError

    fun close() {
        englishToTurkishTranslator?.close()
        turkishToEnglishTranslator?.close()
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        isInitialized = false
    }

    fun getAvailableLanguages(): List<String> {
        return TranslateLanguage.getAllLanguages()
    }

    suspend fun isModelDownloaded(langCode: String): Boolean {
        return try {
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langCode).build()
            modelManager.isModelDownloaded(model).await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadModel(
        langCode: String, 
        requireWifi: Boolean = false,
        showNotification: Boolean = true
    ): String? {
        android.util.Log.d("TranslationManager", "downloadModel started for: $langCode, requireWifi: $requireWifi")
        
        return try {
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langCode).build()
            
            // Check if already downloaded
            val alreadyDownloaded = try {
                modelManager.isModelDownloaded(model).await()
            } catch (e: Exception) {
                false
            }
            
            if (alreadyDownloaded) {
                android.util.Log.d("TranslationManager", "Model $langCode already downloaded")
                if (showNotification) {
                    notificationManager?.showDownloadComplete(langCode)
                }
                return null // Already downloaded, success
            }
            
            val conditions = DownloadConditions.Builder()
                .apply { if (requireWifi) requireWifi() }
                .build()
            
            if (showNotification) {
                notificationManager?.showDownloadStarted(langCode)
            }
            
            onProgressUpdate?.invoke(langCode, 0)
            android.util.Log.d("TranslationManager", "Starting download for: $langCode")
            
            // Use timeout to prevent hanging forever (90 seconds)
            withTimeout(90_000L) {
                onProgressUpdate?.invoke(langCode, 30)
                android.util.Log.d("TranslationManager", "Calling modelManager.download() for: $langCode")
                modelManager.download(model, conditions).await()
            }
            
            android.util.Log.d("TranslationManager", "Download completed for: $langCode")
            onProgressUpdate?.invoke(langCode, 100)
            
            if (showNotification) {
                notificationManager?.showDownloadComplete(langCode)
            }
            
            null // Success
        } catch (e: TimeoutCancellationException) {
            android.util.Log.e("TranslationManager", "Download timeout for: $langCode")
            val errorMsg = "İndirme zaman aşımına uğradı. Lütfen tekrar deneyin."
            if (showNotification) {
                notificationManager?.showDownloadError(langCode, errorMsg)
            }
            errorMsg
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.d("TranslationManager", "Download cancelled for: $langCode")
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            android.util.Log.e("TranslationManager", "downloadModel failed for: $langCode", e)
            val errorMsg = when {
                e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                    "Google sunucuları meşgul. Lütfen birkaç dakika sonra tekrar deneyin."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ağ hatası. İnternet bağlantınızı kontrol edin."
                e.message?.contains("storage", ignoreCase = true) == true ->
                    "Yeterli depolama alanı yok."
                e.message?.contains("wifi", ignoreCase = true) == true ->
                    "WiFi bağlantısı gerekli. WiFi'ye bağlanın veya mobil veri izni verin."
                else ->
                    "Model indirilemedi: ${e.message ?: "Bilinmeyen hata"}"
            }
            
            if (showNotification) {
                notificationManager?.showDownloadError(langCode, errorMsg)
            }
            errorMsg
        }
    }
    
    suspend fun deleteModel(langCode: String): String? {
        return try {
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langCode).build()
            modelManager.deleteDownloadedModel(model).await()
            null // Success
        } catch (e: Exception) {
            e.printStackTrace()
            "Model silinemedi: ${e.message ?: "Bilinmeyen hata"}"
        }
    }
}
