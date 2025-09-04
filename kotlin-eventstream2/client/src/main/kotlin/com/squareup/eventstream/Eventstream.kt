package com.squareup.eventstream

import com.squareup.http.defaultMoshi
import com.squareup.moshi.Moshi
import com.squareup.protos.sawmill.EventstreamV2Event
import com.squareup.protos.sawmill.LogEventStreamV2Request
import com.squareup.protos.sawmill.LogEventStreamV2Response
import java.io.IOException
import java.util.concurrent.TimeUnit.MILLISECONDS

private const val DEFAULT_RETRY_COUNT = 2

public class Eventstream(
  private val eventstreamService: EventstreamService,
  private val moshi: Moshi = defaultMoshi
) {
  /**
   * Sends the given events to Eventstream. Returns whether the request was sent successfully.
   */
  public fun sendEvents(
    events: List<EventstreamEvent>,
    logger: Logger = Logger.NO_OP,
    retryCount: Int = DEFAULT_RETRY_COUNT
  ): Boolean {
    check(retryCount >= 1)

    val batches = events.chunked(EVENTSTREAM_BATCH_SIZE)
    batches.forEachIndexed { index, batch ->
      val success =
        sendBatch(
          batch = batch,
          logger = logger,
          retryCount = retryCount,
          batchNumber = index + 1,
          batchCount = batches.size
        )

      if (!success) return false
    }

    return true
  }

  /**
   * Sends the given events to Eventstream. Returns whether the request was sent successfully.
   */
  private fun sendBatch(
    batch: List<EventstreamEvent>,
    logger: Logger,
    retryCount: Int,
    batchNumber: Int,
    batchCount: Int
  ): Boolean {
    check(retryCount >= 1)

    val request = LogEventStreamV2Request(events = batch.map { it.toV2() })

    repeat(retryCount) {
      logger.info("Send eventstream events, attempt: ${it + 1}, retries: $retryCount")

      val response = sendNetworkRequest(request, logger)
      if (response != null) {
        logger.info(
          "Send eventstream events success for attempt: ${it + 1}, " +
            "events: ${response.success_count}, batch $batchNumber of $batchCount"
        )
        return true
      } else {
        logger.info("Send eventstream events failed , attempt: ${it + 1}, retries: $retryCount")
      }
    }

    return false
  }

  private fun sendNetworkRequest(
    request: LogEventStreamV2Request,
    logger: Logger
  ): LogEventStreamV2Response? {
    val rawResponse =
      try {
        eventstreamService
          .logEvents(request)
          .execute()
      } catch (exception: IOException) {
        // E.g. a timeout. Ignore the error and retry when necessary.
        logger.error("An exception occurred while sending Eventstream events.", exception)
        null
      }

    val response = rawResponse?.body()
    if (response == null) {
      logger.info("Events could not be sent: ${rawResponse?.code()}")
      return null
    }

    check(response.invalid_count == null || response.invalid_count == 0) {
      "There were malformed eventstream events: \nResponse: $response\n\nRequest: $request"
    }

    if ((response.failure_count ?: 0) > 0) {
      logger.info("Events failed to be published: ${response.failure_count}")
      return null
    }

    check((response.success_count ?: request.events.size) == request.events.size) {
      "The success count didn't match the number of events: \n" +
        "Response: $response\n\nRequest: $request"
    }

    return response
  }

  private fun EventstreamEvent.toV2(): EventstreamV2Event =
    EventstreamV2Event(
      catalog_name = catalogName,
      app_name = appName,
      recorded_at_usec = MILLISECONDS.toMicros(recordedAtMs),
      uuid = uuid.toString(),
      json_data = moshi.adapter<Any>(event::class.java).toJson(event)
    )

  public companion object {
    public const val EVENTSTREAM_BATCH_SIZE: Int = 3_000
  }
}

/**
 * Sends the given events to Eventstream. If any of the potentially multiple requests fails, then
 * this method throws an error unlike [Eventstream.sendEvents].
 */
@Suppress("unused")
public fun Eventstream.sendEventsOrFail(
  events: List<EventstreamEvent>,
  logger: Logger = Logger.NO_OP,
  retryCount: Int = DEFAULT_RETRY_COUNT
) {
  if (!sendEvents(events, logger, retryCount)) {
    throw RuntimeException("Sending events to Eventstream failed.")
  }
}
