package xyz.block.idea.telemetry.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.squareup.eventstream.Eventstream
import com.squareup.eventstream.EventstreamService
import xyz.block.idea.telemetry.events.SyncEvent
import xyz.block.idea.telemetry.events.SyncResult
import xyz.block.idea.telemetry.events.SyncResult.SyncFailed
import xyz.block.idea.telemetry.events.SyncResult.SyncSucceeded
import xyz.block.idea.telemetry.submitters.TelemetrySubmitter
import xyz.block.idea.telemetry.submitters.eventstream.EventstreamSubmitter
import xyz.block.idea.telemetry.submitters.googleforms.GoogleFormsSubmitter
import xyz.block.idea.telemetry.submitters.googleforms.GoogleFormsUrlParser
import xyz.block.idea.telemetry.util.intellij.ANDROID_PLUGIN_ID
import xyz.block.idea.telemetry.util.intellij.BLOCK_TELEMETRY_PLUGIN_ID
import xyz.block.idea.telemetry.util.intellij.INTELLIJ_CORE_ID
import xyz.block.idea.telemetry.util.intellij.KOTLIN_PLUGIN_ID
import xyz.block.idea.telemetry.util.intellij.getPluginVersion
import java.io.InputStreamReader
import java.util.Properties

internal class Analytics(private val project: Project) {

  // This is calculated once and on demand.
  // Changes to the config file will require an IJ restart.
  private val gradleProperties: Properties by lazy {
    val properties = Properties()
    project.getFile(CONFIG_FILE_PATH)?.let { file ->
      InputStreamReader(file.inputStream).use {
        properties.load(it)
      }
    }
    properties
  }

  private val configProperties: Properties by lazy {
    val b = gradleProperties.getProperty(DELEGATE_FILE_PATH)
    if (b != null) {
      val properties = Properties()
      project.getFile(b)?.let { file ->
        InputStreamReader(file.inputStream).use {
          properties.load(it)
        }
      }

      // If the user specified a delegate file, use those properties
      properties
    } else {
      // Otherwise, use the original
      gradleProperties
    }
  }

  private val endpoint: String by lazy {
    configProperties.getProperty(ENDPOINT_PROPERTY_NAME).orEmpty()
  }

  private val telemetrySubmitter: TelemetrySubmitter? by lazy {
    // Can't do anything if there's no endpoint in the repo's properties file
    if (endpoint.isBlank()) {
      thisLogger().warn("No endpoint found in $CONFIG_FILE_PATH. Set one with '$ENDPOINT_PROPERTY_NAME=...'")
      return@lazy null
    }

    val endpoint = endpoint.ensurePrefix()

    when {
      endpoint.contains("docs.google.com") -> {
        thisLogger().info("Detected Google Forms URL - using Google Forms submitter")
        val parsedUrl = GoogleFormsUrlParser.parse(endpoint)
        if (parsedUrl != null) {
          GoogleFormsSubmitter(parsedUrl)
        } else {
          thisLogger().error("Failed to initialize Google Forms submitter with endpoint: $endpoint")
          null
        }
      }

      else -> {
        thisLogger().info("Using eventstream submitter for endpoint: $endpoint")
        val es2 = Eventstream(EventstreamService(true, endpoint))
        EventstreamSubmitter(es2)
      }
    }
  }

  private val androidStudioVersion: String = getPluginVersion(ANDROID_PLUGIN_ID)
  private val intelliJCoreVersion: String = getPluginVersion(INTELLIJ_CORE_ID)
  private val kotlinGradlePluginVersion: String = getPluginVersion(KOTLIN_PLUGIN_ID)
  private val blockPluginVersion: String = getPluginVersion(BLOCK_TELEMETRY_PLUGIN_ID)
  private val operatingSystemArchitecture: String = System.getProperty("os.arch")

  // The user's LDAP to upload with the events. There was some discussion around using a short one-way
  // SHA-1 hash for security/privacy instead, but we lose out on cross-referencing statistics from
  // teams/verticals. If we wish to enable in the future, here's how to do it:
  // Hashing.sha1().hashString(user).toString().take(7)
  private val user: String = System.getProperty("user.name")

  fun recordSyncEvent(syncResult: SyncResult) {
    val submitter = telemetrySubmitter
    if (submitter == null) {
      thisLogger().warn("No telemetry submitter available - skipping event")
      return
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      val syncEvent = SyncEvent(
        syncType = syncResult.resultName,
        syncTime = syncResult.totalDuration,
        configureIncludedBuildsDuration = if (syncResult is SyncSucceeded) syncResult.configureIncludedBuildsDuration else -1,
        configureRootProjectDuration = if (syncResult is SyncSucceeded) syncResult.configureRootProjectDuration else -1,
        gradleExecutionDuration = if (syncResult is SyncSucceeded) syncResult.gradleExecutionDuration else -1,
        gradleDuration = if (syncResult is SyncSucceeded) syncResult.gradleDuration else -1,
        ideDuration = if (syncResult is SyncSucceeded) syncResult.ideDuration else -1,
        jvmTotalMemory = Runtime.getRuntime().totalMemory().toString(),
        jvmFreeMemory = Runtime.getRuntime().freeMemory().toString(),
        availableProcessors = Runtime.getRuntime().availableProcessors().toLong(),
        cpuName = readCpuName(),
        numberOfModules = if (syncResult is SyncSucceeded) syncResult.projectCount.toLong() else -1,
        activeWorkspace = null,
        errorMessage = if (syncResult is SyncFailed) syncResult.exception.message else null,
        studioVersion = androidStudioVersion,
        agpVersion = null,
        gradleVersion = syncResult.gradleVersion?.version,
        intellijCoreVersion = intelliJCoreVersion,
        // prefix version with plugin ID so we can differentiate it from sa-toolkit metrics
        toolkitVersion = "$BLOCK_TELEMETRY_PLUGIN_ID-$blockPluginVersion",
        userLdap = user,
        osSystemArchitecture = operatingSystemArchitecture,
        artifactSyncEnabled = false,
        activeRootProjectName = project.name,
        saToolboxChannel = null,
        syncTraceId = syncResult.buildTraceId?.toString(),
      )

      thisLogger().info("Sync $syncResult. Recording event via ${submitter.javaClass.simpleName}")

      if (!submitter.submitEvent(syncEvent)) {
        thisLogger().error("Recording sync $syncResult event failed.")
      }
    }
  }

  companion object {
    private const val CONFIG_FILE_PATH: String = "gradle.properties"
    private const val DELEGATE_FILE_PATH: String = "ide-metrics-plugin.config-file"
    private const val ENDPOINT_PROPERTY_NAME: String = "ide-metrics-plugin.event-stream-endpoint"

    @JvmStatic
    fun get(project: Project): Analytics = project.getService(Analytics::class.java)

    val Project.analyticsService: Analytics get() = get(this)

    private fun readCpuName(): String = try {
      val command = GeneralCommandLine("sysctl", "-n", "machdep.cpu.brand_string")
      ExecUtil.execAndGetOutput(command).stdout.trim()
    } catch (_: Exception) {
      "Unknown"
    }

    private fun Project.getFile(filePath: String): VirtualFile? {
      val rootDir = guessProjectDir()
      if (rootDir == null) {
        thisLogger().warn("The project root directory is null - skipping")
        return null
      }
      val file = rootDir.findFile(filePath)
      if (file == null) {
        thisLogger().info("The file at $filePath is missing")
      }
      return file
    }

    private fun String.ensurePrefix(prefix: String = "https://"): String {
      return if (startsWith(prefix)) this else "$prefix$this"
    }
  }
}
