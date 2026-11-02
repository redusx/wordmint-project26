package com.example.wordstore.data

import com.example.wordstore.data.remote.api.DictionaryApi
import com.example.wordstore.data.remote.model.DictionaryResponseV1
import com.example.wordstore.data.remote.model.EntryV1
import com.example.wordstore.data.remote.model.PronunciationV1
import com.example.wordstore.data.remote.model.SenseV1
import com.example.wordstore.data.repository.RemoteDictionaryRepository
import com.example.wordstore.domain.model.DictionaryStatus
import com.example.wordstore.domain.model.LookupResult
import com.example.wordstore.domain.model.WordLookup
import com.example.wordstore.util.TextTranslator
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteDictionaryRepositoryTest {
    private class FakeApi(val respond: (String) -> Response<DictionaryResponseV1>) : DictionaryApi {
        val asked = mutableListOf<String>()
        override suspend fun getDefinition(word: String): Response<DictionaryResponseV1> {
            asked += word
            return respond(word)
        }
    }

    /** Marks each translation with its direction, so tests can see which way text went. */
    private val translator = TextTranslator { text, from, to -> Result.success("$text[$from>$to]") }
    private val noPacks = TextTranslator { _, _, _ -> Result.failure(IllegalStateException("no pack")) }

    private val entry = EntryV1(
        partOfSpeech = "adjective",
        pronunciations = listOf(PronunciationV1("enpr", "x"), PronunciationV1("ipa", "/ɪˈfɛm/")),
        senses = listOf(
            SenseV1("Short lived.", listOf("Fame is short."), null, null),
            SenseV1("Second.", null, null, null),
            SenseV1("Third.", null, null, null),
            SenseV1("Fourth.", null, null, null)
        ),
        synonyms = listOf("fleeting"),
        antonyms = listOf("lasting")
    )

    private fun success(vararg entries: EntryV1) = Response.success(DictionaryResponseV1("word", entries.toList()))

    private suspend fun lookup(
        api: DictionaryApi,
        query: String = "Ephemeral",
        from: String = "en",
        learning: String = "en",
        known: String = "tr",
        with: TextTranslator = translator
    ): WordLookup {
        val result = RemoteDictionaryRepository(api, with).lookup(query, from, learning, known)
        return (result as LookupResult.Success).lookup
    }

    @Test
    fun `combines definitions, phonetic and translation`() = runTest {
        val result = lookup(FakeApi { success(entry) })
        assertEquals("Ephemeral", result.term)
        assertEquals("Ephemeral[en>tr]", result.translation)
        assertEquals("/ɪˈfɛm/", result.phonetic)
        assertEquals(DictionaryStatus.FOUND, result.dictionaryStatus)
        assertEquals(4, result.meanings.single().definitions.size)
        assertEquals(listOf("fleeting"), result.meanings.single().synonyms)
        assertEquals("en", result.sourceLang)
        assertEquals("tr", result.targetLang)
    }

    @Test
    fun `translates only the first senses, keeping both languages and examples`() = runTest {
        val result = lookup(FakeApi { success(entry) })
        assertEquals(3, result.translatedDefinitions.size)
        val first = result.translatedDefinitions.first()
        assertEquals("Short lived.", first.primaryDefinition)
        assertEquals("Short lived.[en>tr]", first.secondaryDefinition)
        assertEquals("Fame is short.[en>tr]", first.examples.single().secondaryExample)
    }

    @Test
    fun `looks up the whole phrase rather than its first word`() = runTest {
        val api = FakeApi { success(entry) }
        lookup(api, query = "Take Off")
        assertEquals(listOf("take off"), api.asked)
    }

    @Test
    fun `an unknown word answered with 200 and no entries is not found`() = runTest {
        val result = lookup(FakeApi { success() })
        assertEquals(DictionaryStatus.NOT_FOUND, result.dictionaryStatus)
        assertEquals("Ephemeral[en>tr]", result.translation)
        assertTrue(result.meanings.isEmpty())
    }

    @Test
    fun `a 404 is not found and a 500 is a service error`() = runTest {
        fun error(code: Int) = Response.error<DictionaryResponseV1>(code, "".toResponseBody())
        assertEquals(DictionaryStatus.NOT_FOUND, lookup(FakeApi { error(404) }).dictionaryStatus)
        assertEquals(DictionaryStatus.SERVICE_ERROR, lookup(FakeApi { error(500) }).dictionaryStatus)
    }

    @Test
    fun `offline still returns the translation`() = runTest {
        val result = lookup(FakeApi { throw IOException("offline") })
        assertEquals(DictionaryStatus.OFFLINE, result.dictionaryStatus)
        assertEquals("Ephemeral[en>tr]", result.translation)
    }

    @Test
    fun `without packs an English word still gets definitions`() = runTest {
        val result = lookup(FakeApi { success(entry) }, with = noPacks)
        assertNull(result.translation)
        assertEquals(DictionaryStatus.FOUND, result.dictionaryStatus)
        assertEquals("Short lived.", result.translatedDefinitions.first().secondaryDefinition)
    }

    @Test
    fun `typing in the known language makes the translation the term to learn`() = runTest {
        val api = FakeApi { success(entry) }
        val result = lookup(api, query = "kitap", from = "tr")
        assertEquals("kitap[tr>en]", result.term)
        assertEquals("kitap", result.translation)
        assertEquals("en", result.sourceLang)
        assertEquals(listOf("kitap[tr>en]"), api.asked)
    }

    @Test
    fun `typing in the known language with no packs asks for them`() = runTest {
        val result = RemoteDictionaryRepository(FakeApi { success(entry) }, noPacks).lookup("kitap", "tr", "en", "tr")
        assertEquals(LookupResult.PacksRequired, result)
    }

    @Test
    fun `learning another language looks up through English and has no English phonetic`() = runTest {
        val api = FakeApi { success(entry) }
        val result = lookup(api, query = "Buch", from = "de", learning = "de", known = "tr")
        assertEquals(listOf("buch[de>en]"), api.asked)
        assertNull(result.phonetic)
        assertEquals("Short lived.[en>de]", result.translatedDefinitions.first().primaryDefinition)
        assertEquals("Short lived.[en>tr]", result.translatedDefinitions.first().secondaryDefinition)
    }
}
