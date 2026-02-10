package xyz.block.idea.telemetry.submitters.googleforms

import xyz.block.idea.telemetry.events.SyncEvent

/**
 * Parsed representation of a Google Forms prefilled URL.
 *
 * @property formBaseUrl The base URL for form submission (e.g., "https://docs.google.com/forms/d/e/FORM_ID/formResponse")
 * @property fieldMappings List of field mappings extracted from the URL
 */
internal data class GoogleFormsParsedUrl(
  val formBaseUrl: String,
  val fieldMappings: List<GoogleFormsFieldMapping>,
)

/**
 * Represents a mapping between a Google Forms entry field and a SyncEvent property.
 *
 * @property entryId The Google Forms entry ID (e.g., "entry.1234")
 * @property fieldAccessor Function to extract the field value from a SyncEvent
 */
internal data class GoogleFormsFieldMapping(
  val entryId: String,
  val fieldAccessor: (SyncEvent) -> String?,
)
