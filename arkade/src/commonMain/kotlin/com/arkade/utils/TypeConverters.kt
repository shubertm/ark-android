package com.arkade.utils

import androidx.room.TypeConverter
import com.arkade.core.intents.IntentVtxo
import kotlinx.serialization.json.Json

/**
 * Base interface for Room type converters used in the Arkade database.
 *
 * Implementations serialize a value of type [T] to a JSON [String] for storage and
 * deserialize it back on retrieval. Room's `@TypeConverter`-annotated methods should
 * be placed on the concrete implementations.
 *
 * @param T the Kotlin type to convert to/from JSON.
 */
interface ArkadeRoomTypeConverter<T> {
    /**
     * Serializes [value] to a JSON string.
     *
     * @param value the value to serialize.
     * @return a JSON string representation of [value].
     */
    fun from(value: T): String

    /**
     * Deserializes a JSON string back to type [T].
     *
     * @param json the JSON string to deserialize.
     * @return the deserialized value of type [T].
     */
    fun to(json: String): T
}

/**
 * Room type converter for `Map<String, String>` values.
 *
 * Serializes maps to a JSON object string using `kotlinx.serialization` and deserializes
 * them back on read. Registered on the [Database][com.arkade.storage.db.Database] class
 * via `@TypeConverters(StringMapTypeConverter::class)`.
 */
class StringMapTypeConverter : ArkadeRoomTypeConverter<Map<String, String>> {
    /** Encodes [value] as a JSON object string. */
    @TypeConverter
    override fun from(value: Map<String, String>): String = Json.encodeToString(value)

    /** Decodes a JSON object string back to a `Map<String, String>`. */
    @TypeConverter
    override fun to(json: String): Map<String, String> = Json.decodeFromString(json)
}

class StringListTypeConverter : ArkadeRoomTypeConverter<List<String>> {
    @TypeConverter
    override fun from(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    override fun to(json: String): List<String> = Json.decodeFromString(json)
}

class IntentVtxoListTypeConverter : ArkadeRoomTypeConverter<List<IntentVtxo>> {
    @TypeConverter
    override fun from(value: List<IntentVtxo>): String = Json.encodeToString(value)

    @TypeConverter
    override fun to(json: String): List<IntentVtxo> = Json.decodeFromString(json)
}
