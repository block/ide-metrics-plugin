package com.squareup.eventstream

import com.google.common.truth.Truth.assertThat
import com.squareup.http.defaultMoshi
import com.squareup.moshi.adapter
import org.junit.Test
import java.util.UUID

@ExperimentalStdlibApi
class EventstreamEventTest {
  @Test fun `serialization and deserialization works`() {
    val event =
      EventstreamEvent(
        catalogName = "abc",
        appName = "def",
        event = "ghi",
        recordedAtMs = 1L,
        uuid = UUID.fromString("5a18d8ae-b1df-11eb-8529-0242ac130003")
      )

    val adapter = defaultMoshi.adapter<EventstreamEvent>()

    val json = adapter.toJson(event)
    //language=JSON
    assertThat(json).isEqualTo(
      """
        {
          "catalogName":"abc",
          "appName":"def",
          "event":"ghi",
          "recordedAtMs":1,
          "uuid":"5a18d8ae-b1df-11eb-8529-0242ac130003"
        }
      """.filterNot {
        it.isWhitespace()
      }
    )

    assertThat(adapter.fromJson(json)).isEqualTo(event)
  }
}
