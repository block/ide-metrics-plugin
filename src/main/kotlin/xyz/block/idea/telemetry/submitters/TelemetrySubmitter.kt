package xyz.block.idea.telemetry.submitters

import xyz.block.idea.telemetry.events.SyncEvent

/**
 * Interface for submitting telemetry events to different backends.
 */
internal interface TelemetrySubmitter {

  fun submitEvent(event: SyncEvent): Boolean
}
