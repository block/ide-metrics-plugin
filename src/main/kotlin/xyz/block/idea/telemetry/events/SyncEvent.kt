package xyz.block.idea.telemetry.events

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncEvent(
  /** The type of sync event that was detected. */
  @Json(name = "telemetry_android_sync_type") val syncType: String,

  /** Amount of time the sync event took, in milliseconds */
  @Json(name = "telemetry_android_sync_time") val syncTime: Long,

  /** Duration of the configure included builds phase, in milliseconds */
  @Json(name = "configure_included_builds_duration") val configureIncludedBuildsDuration: Long,

  /** Duration of the configure root project phase, in milliseconds */
  @Json(name = "configure_root_project_duration") val configureRootProjectDuration: Long,

  /** Duration of the gradle execution phase, in milliseconds */
  @Json(name = "gradle_execution_duration") val gradleExecutionDuration: Long,

  /** Duration of the total gradle phase (configure + execution), in milliseconds */
  @Json(name = "gradle_duration") val gradleDuration: Long,

  /** Duration of the IDE processing phase, in milliseconds */
  @Json(name = "ide_duration") val ideDuration: Long,

  /** Total amount of memory allocated to the JVM, in bytes */
  @Json(name = "telemetry_android_jvm_total_memory") val jvmTotalMemory: String,

  /** Total amount of memory available from the JVM, in bytes */
  @Json(name = "telemetry_android_jvm_free_memory") val jvmFreeMemory: String,

  /** Number of processors available to the JVM */
  @Json(name = "telemetry_android_available_processors") val availableProcessors: Long,

  /** CPU name, e.g. "Apple M3 Max" */
  @Json(name = "telemetry_android_cpu_name") val cpuName: String,

  /** Number of modules that are in the active workspace */
  @Json(name = "telemetry_android_number_of_modules") val numberOfModules: Long,

  /** The active target workspace (synced modules) that the user is working with  */
  @Json(name = "telemetry_android_active_workspace") val activeWorkspace: String?,

  /** Error message associated with an errored sync state */
  @Json(name = "telemetry_android_error_message") val errorMessage: String?,

  /** Android Studio version for the active environment */
  @Json(name = "telemetry_android_studio_version") val studioVersion: String,

  /** The current sa-toolkit plugin version */
  @Json(name = "telemetry_android_toolkit_version") val toolkitVersion: String,

  /** Android Gradle Plugin version for the active environment */
  @Json(name = "telemetry_android_gradle_plugin_version") val agpVersion: String?,

  /** Gradle build tool version for the active environment */
  @Json(name = "telemetry_android_gradle_version") val gradleVersion: String?,

  /** IntelliJ Core version for the active environment */
  @Json(name = "telemetry_android_intellij_core_version") val intellijCoreVersion: String,

  /** LDAP for the current active user */
  @Json(name = "telemetry_android_user_ldap") val userLdap: String,

  /** CPU architecture for the active environment */
  @Json(name = "telemetry_android_os_system_architecture") val osSystemArchitecture: String,

  /**
   * Flag determining if artifact sync is enabled or not.
   * This feature was formerly codenamed "sandbagging"
   */
  @Json(name = "telemetry_android_sandbagging_enabled") val artifactSyncEnabled: Boolean,

  /** Gradle root project name fro the active environment */
  @Json(name = "telemetry_android_active_root_project_name") val activeRootProjectName: String,

  /** The channel name the user is subscribed to in sa-toolbox */
  @Json(name = "telemetry_android_sa_toolbox_channel") val saToolboxChannel: String?,

  /** The sync trace ID that is also passed to gradle */
  @Json(name = "telemetry_android_sync_trace_id") val syncTraceId: String?,
)