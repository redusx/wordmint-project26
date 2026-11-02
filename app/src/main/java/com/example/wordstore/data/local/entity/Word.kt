package com.example.wordstore.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.example.wordstore.data.remote.model.MeaningDto
import com.example.wordstore.domain.model.TranslatedDefinition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

/** Vocabulary content. Learning progress lives in [CardSchedulingEntity]. */
@Entity(
    tableName = "words",
    indices = [Index(value = ["termKey", "sourceLang", "targetLang"], unique = true)]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The word being learned, in [sourceLang]. Always the front of the card. */
    val term: String,
    /** The meaning, in [targetLang]. */
    val translation: String,
    val sourceLang: String,
    val targetLang: String,
    /** Normalized [term], which the unique index uses to prevent duplicates per language pair. */
    val termKey: String = termKeyOf(term),
    val meanings: List<MeaningDto> = emptyList(),
    val translatedDefinitions: List<TranslatedDefinition> = emptyList(),
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val hint: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        private val WHITESPACE = Regex("\\s+")

        fun termKeyOf(term: String): String = term.trim().replace(WHITESPACE, " ").lowercase(Locale.ROOT)
    }
}

/** A word and its scheduling row, which always exist together. */
data class WordWithCard(
    @Embedded val word: Word,
    @Relation(parentColumn = "id", entityColumn = "wordId")
    val card: CardSchedulingEntity?
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMeaningList(value: List<MeaningDto>?): String =
        gson.toJson(value.orEmpty(), object : TypeToken<List<MeaningDto>>() {}.type)

    @TypeConverter
    fun toMeaningList(value: String): List<MeaningDto> =
        gson.fromJson(value, object : TypeToken<List<MeaningDto>>() {}.type) ?: emptyList()

    @TypeConverter
    fun fromTranslatedDefinitionList(value: List<TranslatedDefinition>?): String =
        gson.toJson(value.orEmpty(), object : TypeToken<List<TranslatedDefinition>>() {}.type)

    @TypeConverter
    fun toTranslatedDefinitionList(value: String): List<TranslatedDefinition> =
        gson.fromJson(value, object : TypeToken<List<TranslatedDefinition>>() {}.type) ?: emptyList()
}
