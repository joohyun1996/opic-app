package com.jooh.opic.core.database

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}
