package com.arkade.utils

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

interface TypeConverter<T> {
    fun from(value: T): String

    fun to(json: String): T
}

class StringMapTypeConverter : com.arkade.utils.TypeConverter<Map<String, String>> {
    @TypeConverter
    override fun from(value: Map<String, String>): String = Json.encodeToString(value)

    @TypeConverter
    override fun to(json: String): Map<String, String> = Json.decodeFromString(json)
}
