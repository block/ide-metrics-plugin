package com.squareup.eventstream

import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * [Eventstream] serializes the given [event] with Moshi to Json and sends the event to Eventstream
 * backend. All [event]s have to have their generated adapters available on the classpath.
 */
@JsonClass(generateAdapter = true)
public data class EventstreamEvent(
  val catalogName: String,
  val appName: String,
  val event: Any,
  val recordedAtMs: Long = System.currentTimeMillis(),
  val uuid: UUID = UUID.randomUUID()
)
