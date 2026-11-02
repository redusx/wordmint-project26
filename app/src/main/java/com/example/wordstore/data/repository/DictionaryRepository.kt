package com.example.wordstore.data.repository

import com.example.wordstore.data.remote.api.DictionaryApi
import com.example.wordstore.data.remote.model.DefinitionDto
import com.example.wordstore.data.remote.model.DictionaryResponseV1
import com.example.wordstore.data.remote.model.MeaningDto
import com.example.wordstore.domain.model.DictionaryStatus
import com.example.wordstore.domain.model.LookupResult
import com.example.wordstore.domain.model.TranslatedDefinition
import com.example.wordstore.domain.model.TranslatedExample
import com.example.wordstore.domain.model.WordLookup
import com.example.wordstore.domain.repository.DictionaryRepository
import com.example.wordstore.util.TextTranslator
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Combines the English-only dictionary with on-device translation.
 *
 * Either side may fail on its own: an offline user still gets a translation, and a user with no
 * language packs still gets definitions. The result always says what is missing.
 */
class RemoteDictionaryRepository(
    private val api: DictionaryApi,
    private val translator: TextTranslator
) : DictionaryRepository {

    override suspend fun lookup(
        query: String,
        fromLang: String,
        learningLang: String,
        knownLang: String
    ): LookupResult = withContext(Dispatchers.IO) {
        val typed = query.trim()

        val term: String
        val translation: String?
        if (fromLang == learningLang) {
            term = typed
            translation = translator.translate(typed, learningLang, knownLang).getOrNull()
        } else {
            // Typed in the known language, so the word to learn must come from the translation.
            term = translator.translate(typed, knownLang, learningLang).getOrNull()
                ?: return@withContext LookupResult.PacksRequired
            translation = typed
        }

        val englishWord = when {
            learningLang == ENGLISH -> term
            knownLang == ENGLISH && translation != null -> translation
            else -> translator.translate(term, learningLang, ENGLISH).getOrNull()
        }

        val (status, response) = if (englishWord == null) {
            DictionaryStatus.NOT_FOUND to null
        } else {
            fetchDefinitions(englishWord)
        }

        val meanings = response?.let(::toMeanings).orEmpty()
        LookupResult.Success(
            WordLookup(
                term = term,
                translation = translation,
                phonetic = if (learningLang == ENGLISH) response?.let(::bestPhonetic) else null,
                meanings = meanings,
                translatedDefinitions = translateDefinitions(meanings, learningLang, knownLang),
                dictionaryStatus = status,
                sourceLang = learningLang,
                targetLang = knownLang
            )
        )
    }

    private suspend fun fetchDefinitions(word: String): Pair<DictionaryStatus, DictionaryResponseV1?> = try {
        val response = api.getDefinition(word.trim().lowercase().trimEnd('.', ',', '!', '?'))
        val body = response.body()
        when {
            response.code() == 404 -> DictionaryStatus.NOT_FOUND to null
            !response.isSuccessful -> DictionaryStatus.SERVICE_ERROR to null
            // The API answers unknown words with HTTP 200 and no entries.
            body?.entries.isNullOrEmpty() -> DictionaryStatus.NOT_FOUND to null
            else -> DictionaryStatus.FOUND to body
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        DictionaryStatus.OFFLINE to null
    } catch (_: RuntimeException) {
        DictionaryStatus.SERVICE_ERROR to null
    }

    private fun toMeanings(response: DictionaryResponseV1): List<MeaningDto> =
        response.entries.orEmpty().mapNotNull { entry ->
            val definitions = entry.senses.orEmpty().mapNotNull { sense ->
                sense.definition?.takeIf(String::isNotBlank)?.let {
                    DefinitionDto(
                        definition = it,
                        example = sense.examples?.firstOrNull(),
                        synonyms = sense.synonyms,
                        antonyms = sense.antonyms
                    )
                }
            }
            if (definitions.isEmpty()) {
                null
            } else {
                MeaningDto(
                    partOfSpeech = entry.partOfSpeech ?: "",
                    definitions = definitions,
                    synonyms = entry.synonyms,
                    antonyms = entry.antonyms
                )
            }
        }

    private fun bestPhonetic(response: DictionaryResponseV1): String? {
        val pronunciations = response.entries.orEmpty().flatMap { it.pronunciations.orEmpty() }
        return (pronunciations.firstOrNull { it.type == "ipa" } ?: pronunciations.firstOrNull())?.text
    }

    private suspend fun translateDefinitions(
        meanings: List<MeaningDto>,
        learningLang: String,
        knownLang: String
    ): List<TranslatedDefinition> {
        // Enough to learn from; translating every sense would make the lookup slow.
        val senses = meanings
            .flatMap { meaning -> meaning.definitions.map { meaning.partOfSpeech to it } }
            .take(MAX_TRANSLATED_DEFINITIONS)

        return senses.map { (partOfSpeech, sense) ->
            TranslatedDefinition(
                primaryDefinition = fromEnglish(sense.definition, learningLang),
                secondaryDefinition = fromEnglish(sense.definition, knownLang),
                partOfSpeech = partOfSpeech,
                examples = sense.example?.takeIf(String::isNotBlank)?.let {
                    listOf(TranslatedExample(fromEnglish(it, learningLang), fromEnglish(it, knownLang)))
                }.orEmpty()
            )
        }
    }

    private suspend fun fromEnglish(text: String, lang: String): String =
        if (lang == ENGLISH) text else translator.translate(text, ENGLISH, lang).getOrDefault(text)

    private companion object {
        const val ENGLISH = "en"
        const val MAX_TRANSLATED_DEFINITIONS = 3
    }
}
