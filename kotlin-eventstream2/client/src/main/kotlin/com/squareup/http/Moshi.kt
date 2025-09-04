package com.squareup.http

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.util.UUID

private val uuidAdapter: JsonAdapter<UUID>
  get() =
    object : JsonAdapter<UUID>() {
      override fun fromJson(reader: JsonReader): UUID {
        return UUID.fromString(reader.nextString())
      }

      override fun toJson(
        writer: JsonWriter,
        value: UUID?
      ) {
        writer.value(value?.toString())
      }
    }

public val defaultMoshi: Moshi by lazy {
  Moshi.Builder()
    .add(UUID::class.java, uuidAdapter)
    .build()
}
