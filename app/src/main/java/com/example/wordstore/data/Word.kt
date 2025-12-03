package com.example.wordstore.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val english: String,
    val turkish: String, // User can still manually enter this
    val sourceLang: String = "en",
    val meanings: List<MeaningDto> = emptyList(),
    val translatedDefinitions: List<TranslatedDefinition> = emptyList(),
    val phonetic: String? = null,
    val audioUrl: String? = null
)

class Converters {
    @TypeConverter
    fun fromMeaningList(value: List<MeaningDto>?): String {
        val gson = Gson()
        val type = object : TypeToken<List<MeaningDto>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toMeaningList(value: String): List<MeaningDto> {
        val gson = Gson()
        val type = object : TypeToken<List<MeaningDto>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromTranslatedDefinitionList(value: List<TranslatedDefinition>?): String {
        val gson = Gson()
        val type = object : TypeToken<List<TranslatedDefinition>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toTranslatedDefinitionList(value: String): List<TranslatedDefinition> {
        val gson = Gson()
        val type = object : TypeToken<List<TranslatedDefinition>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
