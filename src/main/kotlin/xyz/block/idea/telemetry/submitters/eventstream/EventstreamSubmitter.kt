package xyz.block.idea.telemetry.submitters.eventstream

import com.squareup.eventstream.Eventstream
import com.squareup.eventstream.EventstreamEvent
import xyz.block.idea.telemetry.events.SyncEvent
import xyz.block.idea.telemetry.submitters.TelemetrySubmitter

/**
 * TelemetrySubmitter implementation for Eventstream.
 *
 * Wraps the existing Eventstream logic to implement the TelemetrySubmitter interface.
 * Maintains full eventstream functionality including batching and retries.
 *
 * @property eventstream The Eventstream instance
 * @property catalogName The catalog name for eventstream events
 * @property appName The app name for eventstream events
 */
internal class EventstreamSubmitter(
  private val eventstream: Eventstream,
  private val catalogName: String = "telemetry_android",
  private val appName: String = "sa-toolkit-plugin", // TODO: unique app name
) : TelemetrySubmitter {

  /**
   * Submit a sync event via Eventstream.
   *
   * @param event The sync event to submit
   * @return true if submission succeeded, false otherwise
   */
  override fun submitEvent(event: SyncEvent): Boolean {
    val eventstreamEvent = EventstreamEvent(
      catalogName = catalogName,
      appName = appName,
      event = event
    )
    return eventstream.sendEvents(listOf(eventstreamEvent))
  }
}
