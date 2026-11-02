package com.example.wordstore.ui

import com.example.wordstore.domain.model.ExamplePair
import com.example.wordstore.domain.model.contextHint
import com.example.wordstore.domain.model.examples
import com.example.wordstore.domain.model.synonyms
import com.example.wordstore.data.remote.model.DefinitionDto
import com.example.wordstore.data.remote.model.MeaningDto
import com.example.wordstore.domain.model.TranslatedDefinition
import com.example.wordstore.domain.model.TranslatedExample
import com.example.wordstore.fakes.word
import com.example.wordstore.ui.format.IntervalParts
import com.example.wordstore.ui.format.IntervalUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun `intervals choose a readable unit`() {
        assertEquals(IntervalParts(1, IntervalUnit.MINUTES), IntervalParts.of(1))
        assertEquals(IntervalParts(6, IntervalUnit.MINUTES), IntervalParts.of(330_000))
        assertEquals(IntervalParts(3, IntervalUnit.HOURS), IntervalParts.of(3 * hour))
        assertEquals(IntervalParts(8, IntervalUnit.DAYS), IntervalParts.of(8 * day))
        assertEquals(IntervalParts(2, IntervalUnit.MONTHS), IntervalParts.of(61 * day))
        assertEquals(IntervalParts(2, IntervalUnit.YEARS), IntervalParts.of(730 * day))
    }

    @Test
    fun `values just under a unit boundary move to the next unit`() {
        assertEquals(IntervalParts(1, IntervalUnit.HOURS), IntervalParts.of(hour - 1))
        assertEquals(IntervalParts(1, IntervalUnit.DAYS), IntervalParts.of(day - minute))
        assertEquals(IntervalParts(1, IntervalUnit.MONTHS), IntervalParts.of(30 * day - minute))
    }

    @Test
    fun `hint prefers the user's note, then blanks the word in an example`() {
        val meaning = MeaningDto("noun", listOf(DefinitionDto("A set of pages.", "I read a Book today.")))
        val base = word(1, "book").copy(meanings = listOf(meaning))
        assertEquals("I read a _____ today.", base.contextHint())
        assertEquals("From the library", base.copy(hint = "From the library").contextHint())
    }

    @Test
    fun `no hint exists when the example does not contain the word`() {
        val meaning = MeaningDto("verb", listOf(DefinitionDto("Run.", "She went away.")))
        assertNull(word(1, "go").copy(meanings = listOf(meaning)).contextHint())
        assertNull(word(1, "go").contextHint())
    }

    @Test
    fun `examples keep translations and do not repeat`() {
        val meaning = MeaningDto("noun", listOf(DefinitionDto("d", "Same."), DefinitionDto("e", "Other.")))
        val translated = TranslatedDefinition("d", "t", "noun", listOf(TranslatedExample("Same.", "Aynı.")))
        val result = word(1).copy(meanings = listOf(meaning), translatedDefinitions = listOf(translated)).examples
        assertEquals(listOf(ExamplePair("Same.", "Aynı."), ExamplePair("Other.", null)), result)
    }

    @Test
    fun `synonyms merge across levels without repeats`() {
        val meaning = MeaningDto("noun", listOf(DefinitionDto("d", synonyms = listOf("a", "b"))), synonyms = listOf("b", "c"))
        assertEquals(listOf("b", "c", "a"), word(1).copy(meanings = listOf(meaning)).synonyms)
    }
}
