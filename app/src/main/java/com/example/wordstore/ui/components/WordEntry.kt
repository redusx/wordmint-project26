package com.example.wordstore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.wordstore.R
import com.example.wordstore.data.remote.model.MeaningDto
import com.example.wordstore.domain.model.TranslatedDefinition
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme

/**
 * Definitions typeset like a dictionary entry. Parts of speech lead, and senses are numbered
 * because they are an ordered list in the source.
 */
@Composable
fun WordEntryContent(
    meanings: List<MeaningDto>,
    translatedDefinitions: List<TranslatedDefinition>,
    modifier: Modifier = Modifier
) {
    val colors = WordMintTheme.colors
    var senseIndex = 0

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        if (meanings.isEmpty()) {
            translatedDefinitions.forEachIndexed { index, definition ->
                Sense(index + 1, definition.primaryDefinition, definition.secondaryDefinition.takeIf { it != definition.primaryDefinition }, definition.examples.firstOrNull()?.primaryExample, definition.examples.firstOrNull()?.secondaryExample)
            }
        }
        meanings.forEach { meaning ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                if (meaning.partOfSpeech.isNotBlank()) {
                    Text(meaning.partOfSpeech, style = WordMintTheme.type.partOfSpeech, color = colors.inkSecondary)
                }
                meaning.definitions.forEachIndexed { index, definition ->
                    // Translations follow the order of senses across all meanings.
                    val translated = translatedDefinitions.getOrNull(senseIndex++)
                    if (index >= MAX_SENSES_PER_MEANING) return@forEachIndexed
                    val example = translated?.examples?.firstOrNull()
                    Sense(
                        number = index + 1,
                        definition = definition.definition,
                        translation = translated?.secondaryDefinition?.takeIf { it != definition.definition },
                        example = example?.primaryExample ?: definition.example,
                        exampleTranslation = example?.secondaryExample?.takeIf { it != example.primaryExample }
                    )
                }
                val synonyms = (meaning.synonyms.orEmpty() + meaning.definitions.flatMap { it.synonyms.orEmpty() }).distinct()
                if (synonyms.isNotEmpty()) {
                    RelatedWords(stringResource(R.string.word_synonyms), synonyms)
                }
                val antonyms = (meaning.antonyms.orEmpty() + meaning.definitions.flatMap { it.antonyms.orEmpty() }).distinct()
                if (antonyms.isNotEmpty()) {
                    RelatedWords(stringResource(R.string.word_antonyms), antonyms)
                }
            }
        }
    }
}

@Composable
private fun Sense(
    number: Int,
    definition: String,
    translation: String?,
    example: String?,
    exampleTranslation: String?
) {
    val colors = WordMintTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text("$number", style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
            Text(definition, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            if (translation != null) {
                Text(translation, style = MaterialTheme.typography.bodyLarge, color = colors.inkSecondary)
            }
            if (!example.isNullOrBlank()) {
                Text(
                    example,
                    style = WordMintTheme.type.example,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
                if (!exampleTranslation.isNullOrBlank()) {
                    Text(exampleTranslation, style = WordMintTheme.type.example, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
fun RelatedWords(label: String, words: List<String>, modifier: Modifier = Modifier) {
    val colors = WordMintTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        Text(words.take(MAX_RELATED).joinToString(", "), style = MaterialTheme.typography.bodyLarge, color = colors.inkSecondary)
    }
}

private const val MAX_SENSES_PER_MEANING = 5
private const val MAX_RELATED = 8
