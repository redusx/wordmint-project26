package com.example.wordstore.data

import com.example.wordstore.api.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

class DictionaryRepository {

    suspend fun searchWord(query: String, sourceLang: String, targetLang: String): Flow<NetworkResult<WordSearchResult>> {
        return flow {
            emit(NetworkResult.Loading())
            try {
                var englishWord = ""
                var turkishTranslation: String? = null
                var definitions: List<DictionaryEntry>? = null
                var translationError: String? = null

                if (sourceLang == "en" && targetLang == "tr") {
                    // EN -> TR
                    englishWord = query
                    
                    // 1. Get Definition (EN) from Free Dictionary API
                    val defResponse = RetrofitInstance.api.getDefinition(query)
                    if (defResponse.isSuccessful && defResponse.body() != null) {
                        definitions = defResponse.body()
                    }

                    // 2. Translate Word EN -> TR using ML Kit
                    val translationResult = TranslationManager.translateEnToTr(query)
                    if (translationResult.isSuccess) {
                        turkishTranslation = translationResult.getOrNull()
                    } else {
                        translationError = translationResult.exceptionOrNull()?.message
                        turkishTranslation = query // Fallback to original
                    }

                } else if (sourceLang == "tr" && targetLang == "en") {
                    // TR -> EN
                    turkishTranslation = query
                    
                    // 1. Translate Word TR -> EN using ML Kit
                    val translationResult = TranslationManager.translateTrToEn(query)
                    if (translationResult.isSuccess) {
                        englishWord = translationResult.getOrNull() ?: query
                    } else {
                        translationError = translationResult.exceptionOrNull()?.message
                        englishWord = query // Fallback to original
                    }

                    // 2. Get Definition (EN) for the translated word
                    if (englishWord.isNotEmpty()) {
                        val defResponse = RetrofitInstance.api.getDefinition(englishWord)
                        if (defResponse.isSuccessful && defResponse.body() != null) {
                            definitions = defResponse.body()
                        }
                    }
                } else {
                    // Fallback / Other languages
                    englishWord = query
                }

                // Translate definitions if we have them
                var translatedDefinitions: MutableList<TranslatedDefinition>? = null
                
                if (definitions != null) {
                    translatedDefinitions = mutableListOf()
                    var count = 0
                    val maxDefinitionsToTranslate = 3
                    
                    // Flatten definitions
                    val allDefs = definitions.flatMap { entry -> 
                        entry.meanings?.flatMap { meaning -> 
                            meaning.definitions.map { def -> 
                                Triple(def.definition, meaning.partOfSpeech, entry) 
                            } 
                        } ?: emptyList()
                    }

                    for ((defText, partOfSpeech, _) in allDefs) {
                        if (count >= maxDefinitionsToTranslate) break
                        
                        // Translate Definition EN -> TR using ML Kit
                        val translationResult = TranslationManager.translateEnToTr(defText)
                        val translatedText = translationResult.getOrNull() ?: defText
                        
                        val primary: String
                        val secondary: String
                        
                        if (targetLang == "tr") {
                            primary = translatedText
                            secondary = defText
                        } else {
                            // If TR->EN, we show EN definition as primary, TR translation as secondary
                            primary = defText
                            secondary = translatedText
                        }

                        translatedDefinitions.add(TranslatedDefinition(
                            primaryDefinition = primary,
                            secondaryDefinition = secondary,
                            partOfSpeech = partOfSpeech ?: "unknown"
                        ))
                        count++
                    }
                }

                if (englishWord.isNotEmpty() || turkishTranslation != null) {
                     emit(NetworkResult.Success(WordSearchResult(
                        originalQuery = query,
                        englishWord = englishWord,
                        turkishTranslation = turkishTranslation,
                        definitions = definitions,
                        translatedDefinitions = translatedDefinitions,
                        errorMessage = translationError
                    )))
                } else {
                    emit(NetworkResult.Error("Sonuç bulunamadı."))
                }

            } catch (e: IOException) {
                emit(NetworkResult.Error("İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin."))
            } catch (e: HttpException) {
                emit(NetworkResult.Error("Ağ hatası: ${e.code()} - ${e.message()}"))
            } catch (e: Exception) {
                emit(NetworkResult.Error("Beklenmeyen hata: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
            }
        }.flowOn(Dispatchers.IO)
    }
}
