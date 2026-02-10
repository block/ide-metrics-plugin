package xyz.block.idea.telemetry.submitters.googleforms

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.withQuery
import xyz.block.idea.telemetry.events.SyncEvent
import xyz.block.idea.telemetry.submitters.TelemetrySubmitter
import java.net.URI
import java.net.URLEncoder

/**
 * TelemetrySubmitter implementation for Google Forms.
 *
 * Submits sync events by sending HTTP GET requests to a Google Forms URL
 * with query parameters populated from the SyncEvent.
 *
 * @property parsedUrl Parsed Google Forms URL configuration
 * @property service Retrofit service for making HTTP requests
 */
internal class GoogleFormsSubmitter(
  private val parsedUrl: GoogleFormsParsedUrl,
  private val service: GoogleFormsService = GoogleFormsService()
) : TelemetrySubmitter {

  private val logger = thisLogger()

  /**
   * Submit a sync event to Google Forms via HTTP GET request.
   *
   * @param event The sync event to submit
   * @return true if submission succeeded (HTTP 200), false otherwise
   */
  override fun submitEvent(event: SyncEvent): Boolean =
    try {
      val submissionUrl = buildSubmissionUrl(event)
      logger.info("Submitting to Google Forms: $submissionUrl")

      val response = service.submitForm(submissionUrl).execute()

      if (response.isSuccessful) {
        logger.info("Successfully submitted event to Google Forms")
        true
      } else {
        logger.error("Google Forms submission failed with HTTP ${response.code()}")
        false
      }
    } catch (e: Exception) {
      logger.error("Failed to submit event to Google Forms: ${e.message}", e)
      false
    }

  private fun buildSubmissionUrl(event: SyncEvent): String {
    val queryParams = parsedUrl.fieldMappings.map { mapping ->
      val value = mapping.fieldAccessor(event) ?: ""
      "${mapping.entryId}=$value"
    }

    return URI
      .create(parsedUrl.formBaseUrl)
      .withQuery(queryParams.joinToString("&"))
      .toString()
  }
}

