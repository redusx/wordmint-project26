package com.example.wordstore.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val english: String,
    val turkish: String,
    val definitions: List<Definition> = emptyList(), // Store full definitions if needed
    val examples: List<ExamplePair> = emptyList() // Store examples if needed
)

class Converters {
    @TypeConverter
    fun fromDefinitionList(value: List<Definition>?): String {
        val gson = Gson()
        val type = object : TypeToken<List<Definition>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toDefinitionList(value: String): List<Definition> {
        val gson = Gson()
        val type = object : TypeToken<List<Definition>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromExampleList(value: List<ExamplePair>?): String {
        val gson = Gson()
        val type = object : TypeToken<List<ExamplePair>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toExampleList(value: String): List<ExamplePair> {
        val gson = Gson()
        val type = object : TypeToken<List<ExamplePair>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
