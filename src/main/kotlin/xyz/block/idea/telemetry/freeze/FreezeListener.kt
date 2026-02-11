package xyz.block.idea.telemetry.freeze

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.ProjectManager
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message

/**
 * Handles IDE UI freeze events and reports them to Sentry.
 *
 * When the EDT (Event Dispatch Thread) is blocked for an extended period, the IDE captures
 * thread dumps. This handler receives those dumps (via [FreezeListenerRegistrar]'s dynamic
 * proxy), analyzes the EDT stack trace to identify the responsible subsystem, and reports
 * a [FreezeException] to Sentry with full context.
 *
 * Sentry groups freeze events into issues by their origin plugin and top stack frames,
 * so the same code path causing repeated freezes appears as a single issue with event count
 * and duration distribution visible in the Sentry dashboard.
 */
internal class FreezeListener {

  @Volatile
  private var lastDump: ThreadDumpSnapshot? = null

  /** Called when thread dumps are captured during a freeze. */
  fun onDumpedThreads(dump: ThreadDumpSnapshot) {
    lastDump = dump
  }

  /** Called when a UI freeze finishes. */
  fun onFreezeFinished(durationMs: Long) {
    val dump = lastDump
    lastDump = null

    if (dump == null) {
      thisLogger().info("Freeze finished (${durationMs}ms) but no thread dump available.")
      return
    }

    val edtStack = dump.edtStackTrace
    if (edtStack.isNullOrEmpty()) {
      thisLogger().info("Freeze finished (${durationMs}ms) but EDT stack trace is empty.")
      return
    }

    SentryConfig.getInstance().ensureInitialized()

    if (!SentryConfig.getInstance().isInitialized) {
      thisLogger().debug("Sentry not initialized, skipping freeze report.")
      return
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      reportFreeze(durationMs, edtStack, dump.rawDump)
    }
  }

  private fun reportFreeze(
    durationMs: Long,
    edtStack: Array<StackTraceElement>,
    rawDump: String,
  ) {
    try {
      val origin = FreezeAnalyzer.identifyOrigin(edtStack)
      val freezeMessage = FreezeAnalyzer.buildFreezeMessage(origin, edtStack)
      val exception = FreezeException(freezeMessage, durationMs, edtStack)

      val event = SentryEvent(exception).apply {
        level = if (durationMs >= SEVERE_THRESHOLD_MS) SentryLevel.FATAL else SentryLevel.ERROR

        // Fingerprint: groups same-origin + same-code-path freezes as one Sentry issue
        fingerprints = FreezeAnalyzer.buildFingerprint(origin, edtStack)

        // Origin — which plugin or subsystem initiated the work
        setTag("freeze.origin", origin)

        // Duration as a value and a bucket for filtering
        setTag("freeze.duration_ms", durationMs.toString())
        setTag("freeze.duration_bucket", durationBucket(durationMs))

        // Top meaningful frame for quick identification
        val topFrame = FreezeAnalyzer.findTopMeaningfulFrame(edtStack)
        if (topFrame != null) {
          setTag("freeze.top_class", FreezeAnalyzer.shortenClass(topFrame.className))
          setTag("freeze.top_method", topFrame.methodName)
        }

        // IDE info
        val appInfo = ApplicationInfo.getInstance()
        setTag("ide.name", appInfo.versionName)
        setTag("ide.version", appInfo.fullVersion)
        setTag("ide.build", appInfo.build.asString())

        // JVM / system info
        setTag("jvm.xmx_mb", (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toString())
        setTag("jvm.version", System.getProperty("java.version", "unknown"))
        setTag("os.arch", System.getProperty("os.arch", "unknown"))

        // Project info
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
          setTag("project.name", project.name)
        }

        // Extra context (not indexed, but visible on the event detail page)
        setExtra("raw_dump_excerpt", rawDump.take(MAX_RAW_DUMP_LENGTH))
        setExtra("jvm.total_mb", (Runtime.getRuntime().totalMemory() / (1024 * 1024)).toString())
        setExtra("jvm.free_mb", (Runtime.getRuntime().freeMemory() / (1024 * 1024)).toString())
        setExtra("available_processors", Runtime.getRuntime().availableProcessors().toString())
        setExtra("user", System.getProperty("user.name", "unknown"))

        message = Message().apply {
          this.message = freezeMessage
        }
      }

      Sentry.captureEvent(event)
      thisLogger().info("Reported freeze to Sentry: $freezeMessage (${durationMs}ms)")
    } catch (e: Exception) {
      thisLogger().warn("Failed to report freeze to Sentry", e)
    }
  }

  companion object {
    private const val MAX_RAW_DUMP_LENGTH = 10_000
    private const val SEVERE_THRESHOLD_MS = 30_000L

    /** Buckets for filtering freezes by severity in the Sentry dashboard. */
    private fun durationBucket(durationMs: Long): String = when {
      durationMs < 5_000 -> "<5s"
      durationMs < 15_000 -> "5-15s"
      durationMs < 30_000 -> "15-30s"
      durationMs < 60_000 -> "30-60s"
      else -> ">60s"
    }
  }
}
