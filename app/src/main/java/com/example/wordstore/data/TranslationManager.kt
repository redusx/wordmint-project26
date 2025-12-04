package com.example.wordstore.data

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

object TranslationManager {

    private var englishToTurkishTranslator: Translator? = null
    private var turkishToEnglishTranslator: Translator? = null
    
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
            
            // Simulate progress (ML Kit doesn't provide real progress)
            onProgressUpdate?.invoke("en", 25)
            englishToTurkishTranslator?.downloadModelIfNeeded(conditions)?.await()
            
            onProgressUpdate?.invoke("en", 50)
            onProgressUpdate?.invoke("tr", 50)
            turkishToEnglishTranslator?.downloadModelIfNeeded(conditions)?.await()
            
            onProgressUpdate?.invoke("tr", 100)
            
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
        return try {
            if (!isInitialized) initialize()
            
            // Check if model is downloaded
            val isReady = isModelDownloaded(TranslateLanguage.ENGLISH) && 
                          isModelDownloaded(TranslateLanguage.TURKISH)
            
            if (!isReady) {
                return Result.failure(Exception(
                    "Dil modeli hazır değil. Lütfen önce EN/TR modellerini indirin."
                ))
            }
            
            val result = englishToTurkishTranslator?.translate(text)?.await()
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Çeviri yapılamadı."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = if (e.message?.contains("model", ignoreCase = true) == true) {
                "Dil modeli hazır değil. Lütfen Settings'ten modeli indirin."
            } else {
                "Çeviri hatası: ${e.message ?: "Bilinmeyen hata"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun translateTrToEn(text: String): Result<String> {
        return try {
            if (!isInitialized) initialize()
            
            // Check if model is downloaded
            val isReady = isModelDownloaded(TranslateLanguage.ENGLISH) && 
                          isModelDownloaded(TranslateLanguage.TURKISH)
            
            if (!isReady) {
                return Result.failure(Exception(
                    "Dil modeli hazır değil. Lütfen önce EN/TR modellerini indirin."
                ))
            }
            
            val result = turkishToEnglishTranslator?.translate(text)?.await()
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Çeviri yapılamadı."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = if (e.message?.contains("model", ignoreCase = true) == true) {
                "Dil modeli hazır değil. Lütfen Settings'ten modeli indirin."
            } else {
                "Çeviri hatası: ${e.message ?: "Bilinmeyen hata"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    fun getLastError(): String? = lastError

    fun close() {
        englishToTurkishTranslator?.close()
        turkishToEnglishTranslator?.close()
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
            
            // Start progress updates
            onProgressUpdate?.invoke(langCode, 0)
            
            modelManager.download(model, conditions).await()
            
            // Complete
            onProgressUpdate?.invoke(langCode, 100)
            
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
