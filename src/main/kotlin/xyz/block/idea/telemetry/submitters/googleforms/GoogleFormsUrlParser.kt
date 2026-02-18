package xyz.block.idea.telemetry.submitters.googleforms

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.queryParameters
import xyz.block.idea.telemetry.events.SyncEvent
import java.net.URI
import java.net.URLDecoder
import kotlin.reflect.KProperty1

/**
 * Parses Google Forms prefilled URLs and creates field mappings.
 *
 * Extracts entry IDs and placeholder values from query parameters,
 * and maps them to SyncEvent field accessors.
 */
internal object GoogleFormsUrlParser {

  private val PLACEHOLDER_MAPPING: Map<String, (SyncEvent) -> String?> = mapOf(
    "SYNC_TYPE" to SyncEvent::syncType,
    "SYNC_TIME" to SyncEvent::syncTime.toStringMapping(),
    "SYNC_TIME" to SyncEvent::syncTime.toStringMapping(),
    "CONFIGURE_INCLUDED_BUILDS_DURATION" to SyncEvent::configureIncludedBuildsDuration.toStringMapping(),
    "CONFIGURE_ROOT_PROJECT_DURATION" to SyncEvent::configureRootProjectDuration.toStringMapping(),
    "GRADLE_EXECUTION_DURATION" to SyncEvent::gradleExecutionDuration.toStringMapping(),
    "GRADLE_DURATION" to SyncEvent::gradleDuration.toStringMapping(),
    "IDE_DURATION" to SyncEvent::ideDuration.toStringMapping(),
    "JVM_TOTAL_MEMORY" to SyncEvent::jvmTotalMemory,
    "JVM_FREE_MEMORY" to SyncEvent::jvmFreeMemory,
    "AVAILABLE_PROCESSORS" to SyncEvent::availableProcessors.toStringMapping(),
    "CPU_NAME" to SyncEvent::cpuName,
    "NUMBER_OF_MODULES" to SyncEvent::numberOfModules.toStringMapping(),
    "ACTIVE_WORKSPACE" to SyncEvent::activeWorkspace,
    "ERROR_MESSAGE" to SyncEvent::errorMessage,
    "STUDIO_VERSION" to SyncEvent::studioVersion,
    "TOOLKIT_VERSION" to SyncEvent::toolkitVersion,
    "AGP_VERSION" to SyncEvent::agpVersion,
    "GRADLE_VERSION" to SyncEvent::gradleVersion,
    "INTELLIJ_CORE_VERSION" to SyncEvent::intellijCoreVersion,
    "USER_LDAP" to SyncEvent::userLdap,
    "OS_SYSTEM_ARCHITECTURE" to SyncEvent::osSystemArchitecture,
    "ARTIFACT_SYNC_ENABLED" to SyncEvent::artifactSyncEnabled.toStringMapping(),
    "ACTIVE_ROOT_PROJECT_NAME" to SyncEvent::activeRootProjectName,
    "SA_TOOLBOX_CHANNEL" to SyncEvent::saToolboxChannel,
    "SYNC_TRACE_ID" to SyncEvent::syncTraceId,
  )

  /**
   * Parse a Google Forms prefilled URL and extract field mappings.
   *
   * @param url The prefilled Google Forms URL
   * @return Parsed URL with base URL and field mappings or null if parse failed
   */
  fun parse(url: String): GoogleFormsParsedUrl? {
    try {
      val uri = URI(url)

      if (!uri.host.contains("docs.google.com")) {
        thisLogger().error("URL is not a Google Forms URL: ${uri.host}")
        return null
      }

      val formBaseUrl = url.substringBefore("?").replace("viewform", "formResponse")

      val queryParameters = uri.queryParameters
      if (queryParameters.isEmpty()) {
        thisLogger().error("URL has no query parameters: $uri")
        return null
      }

      val fieldMappings = mapQueryParameters(queryParameters)

      if (fieldMappings.isEmpty()) {
        thisLogger().error("No valid field mappings found in URL. Make sure to use recognized placeholder values.")
        return null
      }

      thisLogger().info("Successfully parsed Google Forms URL with ${fieldMappings.size} field mappings")
      return GoogleFormsParsedUrl(formBaseUrl, fieldMappings)

    } catch (e: Exception) {
      thisLogger().error("Failed to parse Google Forms URL: ${e.message}", e)
      throw IllegalArgumentException("Invalid Google Forms URL: ${e.message}", e)
    }
  }

  private fun mapQueryParameters(queryParameters: Map<String, String>): List<GoogleFormsFieldMapping> =
    queryParameters.mapNotNull { (key, value) ->
      if (key.startsWith("entry.")) {
        val decodedValue = URLDecoder.decode(value, "UTF-8")
        val fieldAccessor = PLACEHOLDER_MAPPING[decodedValue]

        if (fieldAccessor != null) {
          thisLogger().info("Mapped $key -> $decodedValue")
          GoogleFormsFieldMapping(
            entryId = key,
            fieldAccessor = fieldAccessor
          )
        } else {
          thisLogger().warn("Unknown placeholder: $decodedValue (field $key will be skipped)")
          null
        }
      } else {
        thisLogger().warn("Unsupported query parameter. Field $key will be skipped")
        null
      }
    }
}

private fun KProperty1<SyncEvent, Any>.toStringMapping(): (SyncEvent) -> String? = {
  this(it).toString()
}
