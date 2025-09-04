package com.squareup.eventstream

import com.google.common.truth.Truth.assertThat
import com.squareup.protos.sawmill.LogEventStreamV2Request
import com.squareup.protos.sawmill.LogEventStreamV2Response
import org.junit.Test
import retrofit2.Call
import retrofit2.mock.Calls
import java.lang.reflect.Proxy
import kotlin.test.assertFailsWith

class EventstreamTest {
  @Test fun `fails for malformed request`() {
    val eventstream = eventstream(LogEventStreamV2Response(invalid_count = 1))

    assertFailsWith<IllegalStateException> {
      eventstream.sendEvents(oneEvent(), logger())
    }
  }

  @Test fun `retries on failure`() {
    val eventstream =
      eventstream(
        LogEventStreamV2Response(failure_count = 1),
        LogEventStreamV2Response(success_count = 1)
      )

    val event = EventstreamEvent("", "", "")
    assertThat(eventstream.sendEvents(listOf(event), logger())).isTrue()
  }

  @Test fun `returns false for repeated failures`() {
    val eventstream =
      eventstream(
        LogEventStreamV2Response(failure_count = 1),
        LogEventStreamV2Response(failure_count = 1)
      )

    assertThat(eventstream.sendEvents(oneEvent(), logger())).isFalse()
  }

  @Test fun `does not retry when specified`() {
    val eventstream =
      eventstream(
        LogEventStreamV2Response(failure_count = 1)
      )

    assertThat(eventstream.sendEvents(oneEvent(), logger(), retryCount = 1)).isFalse()
  }

  @Test fun `fails if the success count doesn't match the number of events`() {
    val eventstream =
      eventstream(
        LogEventStreamV2Response(success_count = 2)
      )

    assertFailsWith<IllegalStateException> {
      // Only one event passed in, but success count is 2.
      eventstream.sendEvents(oneEvent(), logger())
    }
  }

  @Test fun `multiple events are sent in one request`() {
    val eventstream =
      eventstream(
        LogEventStreamV2Response(success_count = 3)
      )

    val events = List(3) { EventstreamEvent("$it", "$it", "$it") }
    eventstream.sendEvents(events, logger())
  }

  private fun eventstream(vararg responses: LogEventStreamV2Response): Eventstream =
    Eventstream(
      eventstreamService =
        object : EventstreamService {
          private var callCount = 0

          override fun logEvents(request: LogEventStreamV2Request): Call<LogEventStreamV2Response> {
            return Calls.response(responses[callCount++])
          }
        }
    )

  private fun logger(): Logger {
    return Proxy.newProxyInstance(
      this::class.java.classLoader,
      arrayOf(Logger::class.java)
    ) { _, _, _ -> } as Logger
  }

  private fun oneEvent() = listOf(EventstreamEvent("", "", ""))
}
