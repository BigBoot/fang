/*
 * Copyright (C) Marco Kirchner - All Rights Reserved
 * Unauthorized copying of this file, via any medium is prohibited
 * Proprietary and confidential
 * Written by Marco Kirchner <marco@kirchner.pw>, 2017
 */

package de.bigboot.ggtools.fang.api.mistforge

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.text.SimpleDateFormat
import java.util.*


class DateAdapter(format: String): JsonAdapter<Date>() {
    val dateFormat = SimpleDateFormat(format, Locale.US)

    override fun fromJson(reader: JsonReader): Date? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.skipValue()
            return null
        }
        return dateFormat.parse(reader.nextString())
    }

    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value == null)
            writer.nullValue()
        else
            writer.value(dateFormat.format(value))
    }
}