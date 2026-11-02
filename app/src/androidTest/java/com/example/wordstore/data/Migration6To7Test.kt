package com.example.wordstore.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wordstore.data.local.database.AppDatabase
import com.example.wordstore.data.local.database.Migration6To7
import com.example.wordstore.domain.scheduling.CardState
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {
    private val dbName = "migration-test"
    private val migratedAt = 1_700_000_000_000L

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun meaning(pos: String, definition: String) =
        """[{"partOfSpeech":"$pos","definitions":[{"definition":"$definition"}]}]"""

    private fun definition(primary: String, secondary: String, exampleA: String, exampleB: String) =
        """[{"primaryDefinition":"$primary","secondaryDefinition":"$secondary","partOfSpeech":"noun",""" +
            """"examples":[{"primaryExample":"$exampleA","secondaryExample":"$exampleB"}]}]"""

    private fun insertV6(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        english: String,
        turkish: String,
        source: String = "en",
        target: String = "tr",
        meanings: String = "[]",
        translated: String = "[]",
        phonetic: String? = null
    ) {
        db.insert(
            "words",
            SQLiteDatabase.CONFLICT_ABORT,
            ContentValues().apply {
                put("english", english)
                put("turkish", turkish)
                put("sourceLang", source)
                put("targetLang", target)
                put("meanings", meanings)
                put("translatedDefinitions", translated)
                put("phonetic", phonetic)
            }
        )
    }

    private fun migrate(seed: (androidx.sqlite.db.SupportSQLiteDatabase) -> Unit) =
        helper.createDatabase(dbName, 6).use(seed).let {
            // Validates the result against the exported 7.json schema.
            helper.runMigrationsAndValidate(dbName, 7, true, Migration6To7 { migratedAt })
        }

    private fun openRoom(): AppDatabase = Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppDatabase::class.java,
        dbName
    ).addMigrations(Migration6To7 { migratedAt }).allowMainThreadQueries().build()

    private fun withRoom(block: (AppDatabase) -> Unit) {
        val room = openRoom()
        try {
            block(room)
        } finally {
            room.close()
        }
    }

    @Test
    fun keepsEveryWordAndItsContent() {
        migrate { db ->
            insertV6(db, "resilient", "dayanıklı", meanings = meaning("adjective", "Springs back."), phonetic = "/ɹɪˈzɪl.jənt/")
            insertV6(db, "ephemeral", "geçici")
        }.close()

        withRoom { room ->
            val words = runBlocking { room.wordDao().observeAll().first() }.map { it.word }
            assertEquals(setOf("resilient", "ephemeral"), words.map { it.term }.toSet())
            val resilient = words.first { it.term == "resilient" }
            assertEquals("dayanıklı", resilient.translation)
            assertEquals("en", resilient.sourceLang)
            assertEquals("tr", resilient.targetLang)
            assertEquals("/ɹɪˈzɪl.jənt/", resilient.phonetic)
            assertEquals("Springs back.", resilient.meanings.single().definitions.single().definition)
            assertEquals(migratedAt, resilient.createdAt)
        }
    }

    @Test
    fun givesEveryWordANewCardDueNow() {
        migrate { db ->
            insertV6(db, "one", "bir")
            insertV6(db, "two", "iki")
        }.close()

        withRoom { room ->
            val cards = runBlocking { room.studyDao().candidates() }
            assertEquals(2, cards.size)
            assertTrue(cards.all { it.state == CardState.NEW.value && it.dueAt == migratedAt })
        }
    }

    @Test
    fun normalizesWordsSavedFromTheOtherDirection() {
        migrate { db ->
            insertV6(
                db,
                english = "window",
                turkish = "pencere",
                source = "tr",
                target = "en",
                translated = definition("Işık açıklığı.", "An opening.", "Pencere açık.", "The window is open.")
            )
        }.close()

        withRoom { room ->
            val word = runBlocking { room.wordDao().observeAll().first() }.single().word
            assertEquals("window", word.term)
            assertEquals("pencere", word.translation)
            assertEquals("en", word.sourceLang)
            assertEquals("tr", word.targetLang)
            val definition = word.translatedDefinitions.single()
            assertEquals("An opening.", definition.primaryDefinition)
            assertEquals("Işık açıklığı.", definition.secondaryDefinition)
            assertEquals("The window is open.", definition.examples.single().primaryExample)
            assertEquals("Pencere açık.", definition.examples.single().secondaryExample)
        }
    }

    @Test
    fun mergesDuplicatesKeepingEveryTranslationAndTheRichestContent() {
        migrate { db ->
            insertV6(db, "Book", "kitap")
            insertV6(db, " book ", "defter", meanings = meaning("noun", "Pages."), phonetic = "/bʊk/")
            insertV6(db, "book", "kitap")
        }.close()

        withRoom { room ->
            val word = runBlocking { room.wordDao().observeAll().first() }.single().word
            assertEquals("kitap, defter", word.translation)
            assertEquals("Pages.", word.meanings.single().definitions.single().definition)
            assertEquals("/bʊk/", word.phonetic)
            assertEquals(1, runBlocking { room.studyDao().candidates() }.size)
        }
    }

    @Test
    fun keepsTheSameWordInDifferentLanguagePairsApart() {
        migrate { db ->
            insertV6(db, "chat", "sohbet", target = "tr")
            insertV6(db, "chat", "chat", target = "de")
        }.close()
        withRoom { room ->
            assertEquals(2, runBlocking { room.wordDao().observeAll().first() }.size)
        }
    }

    @Test
    fun keepsMalformedDefinitionsRatherThanFailing() {
        val db = migrate { insertV6(it, "odd", "tuhaf", source = "tr", target = "en", translated = "not json") }
        db.query("SELECT translatedDefinitions FROM words").use {
            it.moveToFirst()
            assertEquals("not json", it.getString(0))
        }
        db.close()
    }

    @Test
    fun emptyDatabaseMigrates() {
        migrate { }.close()
        withRoom { room ->
            assertTrue(runBlocking { room.wordDao().observeAll().first() }.isEmpty())
        }
    }

    @Test
    fun keepsLegacyJsonReadable() {
        val db = migrate {
            insertV6(it, "x", "y", source = "de", target = "tr", translated = definition("a", "b", "c", "d"))
        }
        db.query("SELECT translatedDefinitions FROM words").use {
            it.moveToFirst()
            val json = JsonParser.parseString(it.getString(0)).asJsonArray[0].asJsonObject
            assertEquals("b", json.get("primaryDefinition").asString)
            assertNotNull(json.get("examples"))
        }
        db.close()
    }
}
