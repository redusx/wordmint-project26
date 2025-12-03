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

                if (sourceLang == "en" && targetLang == "tr") {
                    // EN -> TR
                    // 1. Get Definition (EN)
                    val defResponse = RetrofitInstance.api.getDefinition(query)
                    if (defResponse.isSuccessful && defResponse.body() != null) {
                        englishWord = query
                        definitions = defResponse.body()
                    } else {
                        // Even if no definition, we might still want to translate
                        englishWord = query
                    }

                    // 2. Translate EN -> TR
                    val transResponse = RetrofitInstance.translationApi.getTranslation(query, "en|tr")
                    if (transResponse.isSuccessful && transResponse.body() != null) {
                        val response = transResponse.body()!!
                        var bestTranslation = response.responseData.translatedText
                        
                        // Heuristic to find better translation
                        val matches = response.matches
                        if (!matches.isNullOrEmpty()) {
                            // 1. Filter out known bad patterns
                            val candidates = matches.filter { match ->
                                val text = match.translation
                                !text.equals("Ciao Mondo!", ignoreCase = true) &&
                                !text.trim().startsWith("-") &&
                                !text.equals("hola sup", ignoreCase = true) // Specific bad match seen
                            }

                            // 2. If input is single word, prefer single word output
                            val isSingleWordQuery = !query.trim().contains(" ")
                            
                            val bestCandidate = if (isSingleWordQuery) {
                                // Try to find single word candidates
                                val singleWordCandidates = candidates.filter { !it.translation.trim().contains(" ") }
                                singleWordCandidates.maxByOrNull { it.match } ?: candidates.maxByOrNull { it.match }
                            } else {
                                candidates.maxByOrNull { it.match }
                            }

                            if (bestCandidate != null) {
                                bestTranslation = bestCandidate.translation
                            }
                        }
                        
                        turkishTranslation = bestTranslation
                    }

                } else if (sourceLang == "tr" && targetLang == "en") {
                    // TR -> EN
                    // 1. Translate TR -> EN
                    val transResponse = RetrofitInstance.translationApi.getTranslation(query, "tr|en")
                    if (transResponse.isSuccessful && transResponse.body() != null) {
                        var translatedText = transResponse.body()!!.responseData.translatedText
                        
                        // Fix for specific bad translation "Ciao Mondo!" for "merhaba"
                        if (translatedText.equals("Ciao Mondo!", ignoreCase = true)) {
                            val betterMatch = transResponse.body()!!.matches?.firstOrNull { 
                                !it.translation.equals("Ciao Mondo!", ignoreCase = true) 
                            }
                            if (betterMatch != null) {
                                translatedText = betterMatch.translation
                            }
                        }

                        if (translatedText.isNotEmpty()) {
                            englishWord = translatedText
                            turkishTranslation = query // Original was Turkish

                            // 2. Get Definition (EN) for the translated word
                            val defResponse = RetrofitInstance.api.getDefinition(englishWord)
                            if (defResponse.isSuccessful && defResponse.body() != null) {
                                definitions = defResponse.body()
                            }
                        }
                    }
                } else {
                    // Other combinations (fallback to just translation if possible)
                    val pair = "$sourceLang|$targetLang"
                    val transResponse = RetrofitInstance.translationApi.getTranslation(query, pair)
                    if (transResponse.isSuccessful && transResponse.body() != null) {
                        val translatedText = transResponse.body()!!.responseData.translatedText
                        // We don't know which one is English/Turkish easily without logic, 
                        // but for now we can just return what we have.
                        // Assuming the user might be doing something else.
                        // But sticking to the requirement, we mostly care about EN/TR.
                        // Let's just return the translation.
                        turkishTranslation = translatedText // Placeholder usage
                        englishWord = query // Placeholder
                    }
                }

                // Translate definitions if target language is not English (e.g. TR) OR if source is TR (meaning we want TR context)
                var translatedDefinitions: MutableList<TranslatedDefinition>? = null
                
                // Determine the target language for definition translation
                // If user is translating EN->TR, they want TR definitions.
                // If user is translating TR->EN, they likely still want TR definitions (to understand the English word).
                val defTargetLang = if (targetLang != "en") targetLang else if (sourceLang == "tr") "tr" else null
                
                if (definitions != null && defTargetLang != null) {
                    translatedDefinitions = mutableListOf()
                    var count = 0
                    val maxDefinitionsToTranslate = 3
                    
                    // Flatten definitions to iterate easily
                    val allDefs = definitions.flatMap { entry -> 
                        entry.meanings?.flatMap { meaning -> 
                            meaning.definitions.map { def -> 
                                Triple(def.definition, meaning.partOfSpeech, entry) 
                            } 
                        } ?: emptyList()
                    }

                    for ((defText, partOfSpeech, _) in allDefs) {
                        if (count >= maxDefinitionsToTranslate) break
                        
                        val pair = "en|$defTargetLang"
                        val transResponse = RetrofitInstance.translationApi.getTranslation(defText, pair)
                        if (transResponse.isSuccessful && transResponse.body() != null) {
                            val translatedText = transResponse.body()!!.responseData.translatedText
                            
                            // Determine Primary (Output Lang) and Secondary (Other Lang)
                            val primary: String
                            val secondary: String
                            
                            if (targetLang == "tr") {
                                // Output is TR. Primary = Translated (TR), Secondary = Original (EN)
                                primary = translatedText
                                secondary = defText
                            } else {
                                // Output is EN (or other). Primary = Original (EN), Secondary = Translated (TR)
                                // Note: In TR->EN case, defTargetLang is "tr", so translatedText is TR.
                                // We want EN first.
                                primary = defText
                                secondary = translatedText
                            }

                            translatedDefinitions.add(TranslatedDefinition(
                                primaryDefinition = primary,
                                secondaryDefinition = secondary,
                                partOfSpeech = partOfSpeech ?: "unknown"
                            ))
                        } else {
                             // Fallback
                             translatedDefinitions.add(TranslatedDefinition(
                                primaryDefinition = defText,
                                secondaryDefinition = defText,
                                partOfSpeech = partOfSpeech ?: "unknown"
                            ))
                        }
                        count++
                    }
                }

                if (englishWord.isNotEmpty() || turkishTranslation != null) {
                     emit(NetworkResult.Success(WordSearchResult(
                        originalQuery = query,
                        englishWord = englishWord,
                        turkishTranslation = turkishTranslation,
                        definitions = definitions,
                        translatedDefinitions = translatedDefinitions
                    )))
                } else {
                    emit(NetworkResult.Error("Could not find result."))
                }

            } catch (e: IOException) {
                emit(NetworkResult.Error("No Internet Connection"))
            } catch (e: HttpException) {
                emit(NetworkResult.Error("Network Error: ${e.message()}"))
            } catch (e: Exception) {
                emit(NetworkResult.Error("Unexpected Error: ${e.localizedMessage}"))
            }
        }.flowOn(Dispatchers.IO)
    }
}
