package com.example.wordstore.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.wordstore.domain.repository.PackFailure
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

fun interface TextTranslator {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String>
}

sealed interface PackDownloadResult {
    data object Success : PackDownloadResult
    data class Failure(val reason: PackFailure) : PackDownloadResult
}

object TranslationManager : TextTranslator {
    private const val TAG = "TranslationManager"
    private const val DOWNLOAD_TIMEOUT_MILLIS = 5 * 60_000L

    private val translators = ConcurrentHashMap<String, Translator>()

    // Holds only the application context, which lives as long as this object.
    @SuppressLint("StaticFieldLeak")
    private var notifications: ModelDownloadNotificationManager? = null

    fun initialize(context: Context) {
        if (notifications == null) notifications = ModelDownloadNotificationManager(context.applicationContext)
    }

    private fun translatorFor(sourceLang: String, targetLang: String): Translator =
        translators.getOrPut("$sourceLang-$targetLang") {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build()
            )
        }

    override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        if (sourceLang == targetLang) return Result.success(text)
        return try {
            // Checked first because the translator would otherwise start an unrequested download.
            if (!isModelDownloaded(sourceLang) || !isModelDownloaded(targetLang)) {
                Result.failure(IllegalStateException("Language pack missing: $sourceLang, $targetLang"))
            } else {
                Result.success(translatorFor(sourceLang, targetLang).translate(text).await())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Translation failed: $sourceLang to $targetLang", e)
            Result.failure(e)
        }
    }

    fun availableLanguages(): List<String> = TranslateLanguage.getAllLanguages()

    suspend fun isModelDownloaded(langCode: String): Boolean = try {
        RemoteModelManager.getInstance().isModelDownloaded(model(langCode)).await()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    suspend fun downloadedLanguages(): Set<String> = try {
        RemoteModelManager.getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
            .mapTo(HashSet()) { it.language }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptySet()
    }

    suspend fun downloadModel(langCode: String, requireWifi: Boolean): PackDownloadResult {
        val conditions = DownloadConditions.Builder().apply { if (requireWifi) requireWifi() }.build()
        return try {
            notifications?.showDownloadStarted(langCode)
            withTimeout(DOWNLOAD_TIMEOUT_MILLIS) {
                RemoteModelManager.getInstance().download(model(langCode), conditions).await()
            }
            notifications?.showDownloadComplete(langCode)
            PackDownloadResult.Success
        } catch (_: TimeoutCancellationException) {
            notifications?.showDownloadFailed(langCode)
            PackDownloadResult.Failure(PackFailure.TIMEOUT)
        } catch (e: CancellationException) {
            notifications?.cancel()
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Pack download failed: $langCode", e)
            notifications?.showDownloadFailed(langCode)
            PackDownloadResult.Failure(classify(e))
        }
    }

    private fun classify(error: Exception): PackFailure {
        val message = error.message.orEmpty()
        return when {
            message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) -> PackFailure.BUSY
            message.contains("wifi", ignoreCase = true) -> PackFailure.WIFI_REQUIRED
            message.contains("storage", ignoreCase = true) -> PackFailure.STORAGE
            message.contains("network", ignoreCase = true) -> PackFailure.NETWORK
            else -> PackFailure.UNKNOWN
        }
    }

    private fun model(langCode: String) = TranslateRemoteModel.Builder(langCode).build()
}
