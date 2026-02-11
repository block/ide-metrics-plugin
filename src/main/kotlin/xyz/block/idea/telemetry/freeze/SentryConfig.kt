package xyz.block.idea.telemetry.freeze

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.ProjectManager
import io.sentry.Sentry
import io.sentry.SentryOptions
import xyz.block.idea.telemetry.util.ProjectProperties

/**
 * Application-level service that manages Sentry initialization for freeze reporting.
 *
 * The DSN is resolved from (in order of priority):
 * 1. System property: `ide-metrics-plugin.sentry.dsn` (set in IDE vmoptions)
 * 2. Gradle property: `ide-metrics-plugin.sentry.dsn` (in an open project's gradle.properties)
 */
internal class SentryConfig {

  @Volatile
  private var resolved = false

  @Volatile
  private var sentryActive = false

  val isInitialized: Boolean get() = sentryActive

  /**
   * Lazily initializes Sentry on first call. Resolves the DSN from system properties
   * first, falling back to gradle properties from any open project.
   * Once resolved (whether DSN was found or not), subsequent calls are a no-op.
   */
  fun ensureInitialized() {
    if (resolved) return
    synchronized(this) {
      if (resolved) return

      val dsn = resolveProperty(SENTRY_DSN_PROPERTY)
      if (dsn.isNullOrBlank()) {
        thisLogger().info("No Sentry DSN configured. Freeze reporting is disabled.")
        resolved = true
        return
      }

      Sentry.init { options: SentryOptions ->
        options.dsn = dsn
        options.environment = resolveProperty(SENTRY_ENVIRONMENT_PROPERTY) ?: DEFAULT_ENVIRONMENT
        options.release = resolveRelease()
        options.isEnableUncaughtExceptionHandler = false
        options.isEnableAutoSessionTracking = false
      }

      sentryActive = true
      resolved = true
      thisLogger().info("Sentry initialized for freeze reporting.")
    }
  }

  /** Cached project properties to avoid re-parsing gradle.properties on every call. */
  private var cachedProjectProperties: ProjectProperties? = null

  /**
   * Resolves a property by checking the system property first, then gradle properties
   * from the first open project.
   */
  private fun resolveProperty(key: String): String? {
    System.getProperty(key)?.takeIf { it.isNotBlank() }?.let { return it }
    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return null
    val props = cachedProjectProperties ?: ProjectProperties(project).also { cachedProjectProperties = it }
    return props.get(key)
  }

  companion object {
    const val SENTRY_DSN_PROPERTY = "ide-metrics-plugin.sentry.dsn"
    private const val SENTRY_ENVIRONMENT_PROPERTY = "ide-metrics-plugin.sentry.environment"
    private const val DEFAULT_ENVIRONMENT = "production"

    @JvmStatic
    fun getInstance(): SentryConfig =
      ApplicationManager.getApplication().getService(SentryConfig::class.java)

    private fun resolveRelease(): String {
      val appInfo = ApplicationInfo.getInstance()
      return "${appInfo.versionName}-${appInfo.fullVersion}"
    }
  }
}
