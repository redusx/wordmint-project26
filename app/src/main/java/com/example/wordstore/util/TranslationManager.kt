package com.example.wordstore.util

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

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
        return try {
            if (!isInitialized) {
                return "TranslationManager başlatılmamış."
            }
            
            val conditions = DownloadConditions.Builder()
                .apply { if (requireWifi) requireWifi() }
                .build()

            // Start download notification
            if (showNotification) {
                notificationManager?.showDownloadStarted("EN/TR")
            }
            
            // Progress updates with delays for visibility
            onProgressUpdate?.invoke("en", 0)
            delay(200)
            
            onProgressUpdate?.invoke("en", 30)
            englishToTurkishTranslator?.downloadModelIfNeeded(conditions)?.await()
            delay(200)
            
            onProgressUpdate?.invoke("en", 60)
            onProgressUpdate?.invoke("tr", 30)
            delay(200)
            
            turkishToEnglishTranslator?.downloadModelIfNeeded(conditions)?.await()
            onProgressUpdate?.invoke("tr", 100)
            delay(200)
            
            // Complete notification
            if (showNotification) {
                notificationManager?.showDownloadComplete("EN/TR")
            }
            
            lastError = null
            null // Success
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "İnternet bağlantısı gerekli. Lütfen bağlantınızı kontrol edin."
                e.message?.contains("storage", ignoreCase = true) == true ->
                    "Yeterli depolama alanı yok."
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
        return try {
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langCode).build()
            val conditions = DownloadConditions.Builder()
                .apply { if (requireWifi) requireWifi() }
                .build()
            
            if (showNotification) {
                notificationManager?.showDownloadStarted(langCode)
            }
            
            // Progress updates with delays
            onProgressUpdate?.invoke(langCode, 0)
            delay(300)
            
            onProgressUpdate?.invoke(langCode, 30)
            delay(300)
            
            modelManager.download(model, conditions).await()
            
            onProgressUpdate?.invoke(langCode, 100)
            delay(200)
            
            if (showNotification) {
                notificationManager?.showDownloadComplete(langCode)
            }
            
            null // Success
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "Model indirilemedi: ${e.message ?: "Bilinmeyen hata"}"
            
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
