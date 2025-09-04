package xyz.block.idea.telemetry.gradle

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import xyz.block.idea.telemetry.common.Constants

/**
 * ```
 * // settings.gradle[.kts]
 * plugins {
 *   id("xyz.block.ide-telemetry")
 * }
 * ```
 */
public abstract class IdeTelemetryPlugin : Plugin<Settings> {
  override fun apply(target: Settings): Unit = target.run {
    pluginManager.withPlugin("com.gradle.develocity") {
      extensions.getByType(DevelocityConfiguration::class.java)
        .buildScan { scan ->
          // We do this in buildFinished {} so the property isn't capture as part of configuration cache inputs.
          scan.buildFinished {
            scan.value("Build Trace ID", buildTraceId())
          }
        }
    }
  }

  private fun buildTraceId() = System.getProperty(Constants.BUILD_TRACE_ID_PROPERTY, "unknown")
}