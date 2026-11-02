package com.example.wordstore.data.local.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.wordstore.data.local.entity.Word
import com.google.gson.JsonArray
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/**
 * Moves vocabulary to the learning schema without losing any of it.
 *
 * In version 6 the `english` column always held the headline word and `turkish` its counterpart,
 * whatever the languages really were, and for non-English sources the language columns described
 * the opposite order. Here `term` becomes the headline word and the language columns are corrected
 * to match. Each word also gets a New card, which makes it eligible for study.
 */
class Migration6To7(private val now: () -> Long = System::currentTimeMillis) : Migration(6, 7) {

    private class LegacyRow(
        val id: Long,
        val term: String,
        val translation: String,
        val sourceLang: String,
        val targetLang: String,
        val meanings: String,
        val translatedDefinitions: String,
        val phonetic: String?,
        val audioUrl: String?
    ) {
        val key = Word.termKeyOf(term)
        val richness = meanings.length + translatedDefinitions.length
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        val rows = readRows(db)
        val migratedAt = now()

        db.execSQL(
            """
            CREATE TABLE `words_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `term` TEXT NOT NULL,
                `translation` TEXT NOT NULL,
                `sourceLang` TEXT NOT NULL,
                `targetLang` TEXT NOT NULL,
                `termKey` TEXT NOT NULL,
                `meanings` TEXT NOT NULL,
                `translatedDefinitions` TEXT NOT NULL,
                `phonetic` TEXT,
                `audioUrl` TEXT,
                `hint` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        val merged = rows.groupBy { Triple(it.key, it.sourceLang, it.targetLang) }.values.map(::merge)
        merged.forEach { row ->
            db.insert(
                "words_new",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("id", row.id)
                    put("term", row.term)
                    put("translation", row.translation)
                    put("sourceLang", row.sourceLang)
                    put("targetLang", row.targetLang)
                    put("termKey", row.key)
                    put("meanings", row.meanings)
                    put("translatedDefinitions", row.translatedDefinitions)
                    put("phonetic", row.phonetic)
                    put("audioUrl", row.audioUrl)
                    put("createdAt", migratedAt)
                    put("updatedAt", migratedAt)
                }
            )
        }

        db.execSQL("DROP TABLE `words`")
        db.execSQL("ALTER TABLE `words_new` RENAME TO `words`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_words_termKey_sourceLang_targetLang` " +
                "ON `words` (`termKey`, `sourceLang`, `targetLang`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `card_scheduling` (
                `wordId` INTEGER NOT NULL,
                `state` INTEGER NOT NULL,
                `step` INTEGER,
                `stability` REAL,
                `difficulty` REAL,
                `dueAt` INTEGER NOT NULL,
                `lastReviewAt` INTEGER,
                `reps` INTEGER NOT NULL,
                `lapses` INTEGER NOT NULL,
                `schedulerVersion` TEXT NOT NULL DEFAULT 'fsrs-6',
                PRIMARY KEY(`wordId`),
                FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_scheduling_dueAt` ON `card_scheduling` (`dueAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_scheduling_state` ON `card_scheduling` (`state`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `review_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `wordId` INTEGER,
                `rating` INTEGER NOT NULL,
                `reviewedAt` INTEGER NOT NULL,
                `learningDay` INTEGER NOT NULL,
                `elapsedMillis` INTEGER NOT NULL,
                `durationMillis` INTEGER NOT NULL,
                `previousDueAt` INTEGER NOT NULL,
                `newDueAt` INTEGER NOT NULL,
                `intervalMillis` INTEGER NOT NULL,
                `previousState` INTEGER NOT NULL,
                `newState` INTEGER NOT NULL,
                `previousStep` INTEGER,
                `newStep` INTEGER,
                `previousStability` REAL,
                `newStability` REAL NOT NULL,
                `previousDifficulty` REAL,
                `newDifficulty` REAL NOT NULL,
                `dailyNewGoal` INTEGER NOT NULL,
                `algorithmVersion` TEXT NOT NULL,
                FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_wordId` ON `review_logs` (`wordId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_learningDay` ON `review_logs` (`learningDay`)")

        // Every word starts as New. The queue breaks equal timestamps on ID, so older words come first.
        db.execSQL("INSERT INTO `card_scheduling` (`wordId`, `state`, `dueAt`, `reps`, `lapses`) SELECT `id`, 0, $migratedAt, 0, 0 FROM `words`")
    }

    private fun readRows(db: SupportSQLiteDatabase): List<LegacyRow> {
        val rows = mutableListOf<LegacyRow>()
        db.query(
            "SELECT id, english, turkish, sourceLang, targetLang, meanings, translatedDefinitions, phonetic, audioUrl " +
                "FROM words ORDER BY id"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val legacySource = cursor.getString(3)
                val legacyTarget = cursor.getString(4)
                val isSwapped = legacySource != "en"
                val definitions = cursor.getString(6)
                rows += LegacyRow(
                    id = cursor.getLong(0),
                    term = cursor.getString(1),
                    translation = cursor.getString(2),
                    sourceLang = if (isSwapped) legacyTarget else legacySource,
                    targetLang = if (isSwapped) legacySource else legacyTarget,
                    meanings = cursor.getString(5),
                    translatedDefinitions = if (isSwapped) swapDefinitionSides(definitions) else definitions,
                    phonetic = cursor.getString(7),
                    audioUrl = cursor.getString(8)
                )
            }
        }
        return rows
    }

    /** Keeps the most complete row and every distinct translation among duplicates. */
    private fun merge(duplicates: List<LegacyRow>): LegacyRow {
        val best = duplicates.maxWith(compareBy({ it.richness }, { it.id }))
        if (duplicates.size == 1) return best
        val translations = duplicates.map { it.translation.trim() }.filter { it.isNotEmpty() }.distinct()
        return LegacyRow(
            id = best.id,
            term = best.term,
            translation = translations.joinToString(", "),
            sourceLang = best.sourceLang,
            targetLang = best.targetLang,
            meanings = best.meanings,
            translatedDefinitions = best.translatedDefinitions,
            phonetic = best.phonetic ?: duplicates.firstNotNullOfOrNull { it.phonetic },
            audioUrl = best.audioUrl ?: duplicates.firstNotNullOfOrNull { it.audioUrl }
        )
    }

    /** Primary text must be in the term's language, which changed sides for these rows. */
    private fun swapDefinitionSides(json: String): String = try {
        val definitions = JsonParser.parseString(json).asJsonArray
        val swapped = JsonArray()
        definitions.forEach { element ->
            val definition = element.asJsonObject
            swapMembers(definition.asJsonObject, "primaryDefinition", "secondaryDefinition")
            definition.getAsJsonArray("examples")?.forEach { example ->
                swapMembers(example.asJsonObject, "primaryExample", "secondaryExample")
            }
            swapped.add(definition)
        }
        swapped.toString()
    } catch (_: JsonParseException) {
        json
    } catch (_: IllegalStateException) {
        json
    }

    private fun swapMembers(target: com.google.gson.JsonObject, first: String, second: String) {
        val firstValue = target.get(first)
        val secondValue = target.get(second)
        if (firstValue != null && secondValue != null) {
            target.add(first, secondValue)
            target.add(second, firstValue)
        }
    }
}
