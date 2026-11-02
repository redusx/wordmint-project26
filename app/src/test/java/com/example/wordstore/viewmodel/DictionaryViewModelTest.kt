package com.example.wordstore.viewmodel

import com.example.wordstore.domain.model.DictionaryStatus
import com.example.wordstore.domain.model.LookupResult
import com.example.wordstore.domain.model.WordLookup
import com.example.wordstore.domain.repository.DictionaryRepository
import com.example.wordstore.fakes.FakePacks
import com.example.wordstore.fakes.FakeSettings
import com.example.wordstore.fakes.FakeWordRepository
import com.example.wordstore.fakes.MutableClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val words = FakeWordRepository()
    private val settings = FakeSettings()

    private class FakeDictionary(var result: LookupResult) : DictionaryRepository {
        var lastFrom: String? = null
        override suspend fun lookup(query: String, fromLang: String, learningLang: String, knownLang: String): LookupResult {
            lastFrom = fromLang
            return result
        }
    }

    private val found = LookupResult.Success(
        WordLookup("book", "kitap", "/bʊk/", emptyList(), emptyList(), DictionaryStatus.FOUND, "en", "tr")
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(dictionary: DictionaryRepository, packs: FakePacks = FakePacks()) =
        DictionaryViewModel(dictionary, words, settings, packs, MutableClock())

    @Test
    fun `search fills in the translation for editing`() = runTest(dispatcher) {
        val vm = viewModel(FakeDictionary(found))
        backgroundScope.launch { vm.state.collect {} }
        vm.onQueryChange("book")
        vm.search()
        assertEquals("kitap", vm.state.value.translationDraft)
        assertTrue(vm.state.value.lookup is LookupState.Found)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `saving uses the edited translation and marks the word as saved`() = runTest(dispatcher) {
        val vm = viewModel(FakeDictionary(found))
        backgroundScope.launch { vm.state.collect {} }
        vm.onQueryChange("book")
        vm.search()
        vm.onTranslationChange("defter")
        vm.save()
        val saved = words.words.value.single()
        assertEquals("defter", saved.word.translation)
        assertNotNull(saved.card)
        assertEquals(saved.word.id, vm.state.value.savedWordId)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun `an already saved word is recognized, so it cannot be saved twice`() = runTest(dispatcher) {
        val vm = viewModel(FakeDictionary(found))
        backgroundScope.launch { vm.state.collect {} }
        vm.onQueryChange("book")
        vm.search()
        vm.save()
        vm.clear()
        assertNull(vm.state.value.savedWordId)

        vm.onQueryChange("BOOK")
        vm.search()
        assertNotNull(vm.state.value.savedWordId)
        assertEquals("kitap", vm.state.value.savedTranslation)
        vm.save()
        assertEquals(1, words.words.value.size)
    }

    @Test
    fun `a blank translation cannot be saved`() = runTest(dispatcher) {
        val vm = viewModel(FakeDictionary(found))
        backgroundScope.launch { vm.state.collect {} }
        vm.onQueryChange("book")
        vm.search()
        vm.onTranslationChange("  ")
        assertFalse(vm.state.value.canSave)
        vm.save()
        assertTrue(words.words.value.isEmpty())
    }

    @Test
    fun `swapping direction searches from the known language`() = runTest(dispatcher) {
        val dictionary = FakeDictionary(found)
        val vm = viewModel(dictionary)
        backgroundScope.launch { vm.state.collect {} }
        vm.swapDirection()
        assertEquals("tr", vm.state.value.fromLang)
        vm.onQueryChange("kitap")
        vm.search()
        assertEquals("tr", dictionary.lastFrom)
    }

    @Test
    fun `missing packs are reported, including English as the dictionary's language`() = runTest(dispatcher) {
        settings.setLanguages("de", "tr")
        val vm = viewModel(FakeDictionary(found), FakePacks(downloaded = setOf("tr")))
        backgroundScope.launch { vm.state.collect {} }
        assertEquals(listOf("de", "en"), vm.state.value.missingPacks)
    }

    @Test
    fun `required packs never repeat`() {
        assertEquals(listOf("en", "tr"), DictionaryViewModel.requiredPacks("en", "tr"))
        assertEquals(listOf("de", "tr", "en"), DictionaryViewModel.requiredPacks("de", "tr"))
    }

    @Test
    fun `choosing the other side's language swaps the pair`() = runTest(dispatcher) {
        val vm = viewModel(FakeDictionary(found))
        backgroundScope.launch { vm.state.collect {} }
        vm.setLearningLanguage("tr")
        assertEquals("tr", settings.settings.value.sourceLang)
        assertEquals("en", settings.settings.value.targetLang)
    }
}
