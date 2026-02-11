package xyz.block.idea.telemetry.util

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import java.io.InputStreamReader
import java.util.Properties

/**
 * Reads plugin configuration properties from a project's `gradle.properties`.
 *
 * Supports an optional delegate file: if `gradle.properties` contains a property
 * `ide-metrics-plugin.config-file`, properties are read from that file instead.
 *
 * Results are cached per-project for the lifetime of the [ProjectProperties] instance.
 * Changes to the config file require an IDE restart.
 */
internal class ProjectProperties(private val project: Project) {

  private val gradleProperties: Properties by lazy {
    val properties = Properties()
    project.getFile(GRADLE_PROPERTIES_FILE)?.let { file ->
      InputStreamReader(file.inputStream).use { properties.load(it) }
    }
    properties
  }

  /** The effective properties, accounting for an optional delegate file. */
  private val configProperties: Properties by lazy {
    val delegatePath = gradleProperties.getProperty(DELEGATE_FILE_PROPERTY)
    if (delegatePath != null) {
      val delegateProps = Properties()
      project.getFile(delegatePath)?.let { file ->
        InputStreamReader(file.inputStream).use { delegateProps.load(it) }
      }
      delegateProps
    } else {
      gradleProperties
    }
  }

  /** Returns the value of [key] from the resolved config, or `null` if absent/blank. */
  fun get(key: String): String? =
    configProperties.getProperty(key)?.takeIf { it.isNotBlank() }

  companion object {
    private const val GRADLE_PROPERTIES_FILE = "gradle.properties"
    private const val DELEGATE_FILE_PROPERTY = "ide-metrics-plugin.config-file"

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
  }
}
