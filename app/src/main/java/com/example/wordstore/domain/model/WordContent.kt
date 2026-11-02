package com.example.wordstore.domain.model

import com.example.wordstore.data.local.entity.Word

/** An example sentence and, when available, its translation. */
data class ExamplePair(val text: String, val translation: String?)

val Word.partOfSpeech: String?
    get() = meanings.firstOrNull()?.partOfSpeech ?: translatedDefinitions.firstOrNull()?.partOfSpeech

/** The dictionary's own wording, which is always English. */
val Word.dictionaryDefinition: String?
    get() = meanings.firstNotNullOfOrNull { meaning -> meaning.definitions.firstOrNull()?.definition }

val Word.examples: List<ExamplePair>
    get() {
        val translated = translatedDefinitions.flatMap { definition ->
            definition.examples.map { ExamplePair(it.primaryExample, it.secondaryExample.takeIf { text -> text != it.primaryExample }) }
        }
        val known = translated.map { it.text }.toHashSet()
        val untranslated = meanings
            .flatMap { it.definitions }
            .mapNotNull { it.example?.takeIf(String::isNotBlank) }
            .filter { it !in known }
            .map { ExamplePair(it, null) }
        return (translated + untranslated).distinctBy { it.text }
    }

val Word.synonyms: List<String>
    get() = meanings
        .flatMap { meaning -> meaning.synonyms.orEmpty() + meaning.definitions.flatMap { it.synonyms.orEmpty() } }
        .distinct()

val Word.antonyms: List<String>
    get() = meanings
        .flatMap { meaning -> meaning.antonyms.orEmpty() + meaning.definitions.flatMap { it.antonyms.orEmpty() } }
        .distinct()

/**
 * A nudge that does not give the answer: the user's own note, or else an example sentence with the
 * word blanked out. Null when neither exists, so the card simply offers no hint.
 */
fun Word.contextHint(): String? {
    hint?.takeIf(String::isNotBlank)?.let { return it }
    val pattern = Regex("\\b" + Regex.escape(term.trim()) + "\\w*\\b", RegexOption.IGNORE_CASE)
    return examples.firstOrNull { pattern.containsMatchIn(it.text) }?.text?.replace(pattern, "_____")
}
