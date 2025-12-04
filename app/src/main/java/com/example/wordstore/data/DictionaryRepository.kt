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
                var translatedWord: String? = null
                var definitions: List<DictionaryEntry>? = null
                var translationError: String? = null
                var englishWord = ""

                // Step 1: Translate input to target language
                val translationResult = TranslationManager.translate(query, sourceLang, targetLang)
                if (translationResult.isSuccess) {
                    translatedWord = translationResult.getOrNull()
                } else {
                    translationError = translationResult.exceptionOrNull()?.message
                    translatedWord = query // Fallback to original
                }

                // Step 2: Translate input to English (if not already English)
                // Free Dictionary API only works with English
                if (sourceLang == "en") {
                    englishWord = query
                } else {
                    val toEnglishResult = TranslationManager.translate(query, sourceLang, "en")
                    if (toEnglishResult.isSuccess) {
                        englishWord = toEnglishResult.getOrNull() ?: query
                    } else {
                        englishWord = query // Fallback
                    }
                }

                // Step 3: Get English definitions from Free Dictionary API (v1)
                if (englishWord.isNotEmpty()) {
                    // Clean the word: lowercase, trim, remove punctuation
                    val cleanWord = englishWord.trim()
                        .lowercase()
                        .replace(Regex("[,\\.!\\?]"), "")
                        .split(" ")
                        .firstOrNull() ?: englishWord.lowercase()
                    
                    val defResponse = RetrofitInstance.api.getDefinition(cleanWord)
                    if (defResponse.isSuccessful && defResponse.body() != null) {
                        val v1Response = defResponse.body()!!
                        
                        // Convert v1 API response to old DictionaryEntry format for compatibility
                        // v1 has: entries -> senses (definitions)
                        // We'll create compatible structure for existing code
                        definitions = listOf(DictionaryEntry(
                            word = v1Response.word,
                            phonetic = v1Response.entries.firstOrNull()?.pronunciations?.firstOrNull()?.text,
                            phonetics = v1Response.entries.firstOrNull()?.pronunciations?.map {
                                PhoneticDto(text = it.text, audio = null)
                            },
                            meanings = v1Response.entries.map { entry ->
                                MeaningDto(
                                    partOfSpeech = entry.partOfSpeech,
                                    definitions = entry.senses.map { sense ->
                                        DefinitionDto(
                                            definition = sense.definition,
                                            example = sense.examples?.firstOrNull(), // Take first example for compatibility
                                            synonyms = sense.synonyms,
                                            antonyms = sense.antonyms
                                        )
                                    }
                                )
                            }
                        ))
                    }
                }

                // Step 4: Translate definitions to source and target languages
                var translatedDefinitions: MutableList<TranslatedDefinition>? = null
                
                if (definitions != null) {
                    translatedDefinitions = mutableListOf()
                    var count = 0
                    val maxDefinitionsToTranslate = 2  // Limit to 2 definitions for better UX
                    
                    // Flatten definitions with their examples from v1 API
                    // v1 API has entries -> senses, where each sense has examples array
                    val allDefs = definitions.flatMap { entry -> 
                        entry.meanings?.flatMap { meaning -> 
                            meaning.definitions.map { def -> 
                                // Get examples array from DefinitionDto
                                // For v1 API compatibility, we stored first example in def.example
                                // But we need to get all examples from the senses
                                Triple(def.definition, def.example, meaning.partOfSpeech) 
                            } 
                        } ?: emptyList()
                    }

                    for ((defText, exampleFirst, partOfSpeech) in allDefs) {
                        if (count >= maxDefinitionsToTranslate) break
                        
                        // Translate definition: EN -> sourceLang (primary)
                        val primaryDef = if (sourceLang == "en") {
                            defText
                        } else {
                            val primaryResult = TranslationManager.translate(defText, "en", sourceLang)
                            primaryResult.getOrNull() ?: defText
                        }
                        
                        // Translate definition: EN -> targetLang (secondary)
                        val secondaryDef = if (targetLang == "en") {
                            defText
                        } else {
                            val secondaryResult = TranslationManager.translate(defText, "en", targetLang)
                            secondaryResult.getOrNull() ?: defText
                        }
                        
                        // Translate example (single example from v1 API)
                        val translatedExamples = if (!exampleFirst.isNullOrBlank()) {
                            val primaryExample = if (sourceLang == "en") {
                                exampleFirst
                            } else {
                                val result = TranslationManager.translate(exampleFirst, "en", sourceLang)
                                result.getOrNull() ?: exampleFirst
                            }
                            
                            val secondaryExample = if (targetLang == "en") {
                                exampleFirst
                            } else {
                                val result = TranslationManager.translate(exampleFirst, "en", targetLang)
                                result.getOrNull() ?: exampleFirst
                            }
                            
                            listOf(TranslatedExample(
                                primaryExample = primaryExample,
                                secondaryExample = secondaryExample
                            ))
                        } else {
                            emptyList()
                        }
                        
                        translatedDefinitions.add(TranslatedDefinition(
                            primaryDefinition = primaryDef,
                            secondaryDefinition = secondaryDef,
                            partOfSpeech = partOfSpeech ?: "unknown",
                            examples = translatedExamples
                        ))
                        count++
                    }
                }

                // Build result
                // For compatibility with existing UI, map to englishWord/turkishTranslation
                val resultEnglishWord: String
                val resultTurkishTranslation: String?
                
                when {
                    sourceLang == "en" && targetLang == "tr" -> {
                        resultEnglishWord = query
                        resultTurkishTranslation = translatedWord
                    }
                    sourceLang == "tr" && targetLang == "en" -> {
                        resultEnglishWord = translatedWord ?: query
                        resultTurkishTranslation = query
                    }
                    targetLang == "en" -> {
                        resultEnglishWord = translatedWord ?: query
                        resultTurkishTranslation = query
                    }
                    sourceLang == "en" -> {
                        resultEnglishWord = query
                        resultTurkishTranslation = translatedWord
                    }
                    else -> {
                        // For other pairs (e.g., FR->DE), use englishWord and translatedWord
                        resultEnglishWord = englishWord
                        resultTurkishTranslation = translatedWord
                    }
                }

                if (resultEnglishWord.isNotEmpty() || resultTurkishTranslation != null) {
                     emit(NetworkResult.Success(WordSearchResult(
                        originalQuery = query,
                        englishWord = resultEnglishWord,
                        turkishTranslation = resultTurkishTranslation,
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
