package xyz.block.idea.telemetry.listeners.sync

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.status.SyncStatusListener
import xyz.block.idea.telemetry.events.SyncResult
import xyz.block.idea.telemetry.services.Analytics.Companion.analyticsService
import xyz.block.idea.telemetry.util.now

/**
 * Listens for sync events from the JetBrains Bazel plugin (org.jetbrains.bazel).
 *
 * Registered in bazel-metrics.xml, which is only loaded when the Bazel plugin is installed.
 *
 * The Bazel plugin's public [SyncStatusListener] API only reports start/finish/cancelled, so
 * the exact outcome (succeeded/failed) is taken from [BazelSyncOutcomeTracker], which observes
 * the Sync tool window events; if unavailable, the outcome falls back to "finished". There are
 * no per-phase durations. Note that a "phased" Bazel sync fires two start/finish pairs (one
 * per phase), which results in two events.
 */
internal class BazelSyncListener(private val project: Project) : SyncStatusListener {

  @Volatile
  private var startTimestamp: Long = -1

  override fun syncStarted() {
    thisLogger().info("Bazel syncStarted")
    startTimestamp = now()
    // Instantiating the tracker here (before the Bazel plugin opens its sync console) ensures
    // it observes the sync's build events from the start.
    project.getService(BazelSyncOutcomeTracker::class.java).syncWindowStarted()
  }

  override fun syncFinished(canceled: Boolean) {
    val start = startTimestamp
    startTimestamp = -1
    if (start == -1L) return

    val finish = now()
    val observedOutcome = project.getService(BazelSyncOutcomeTracker::class.java).consumeOutcome()
    val result = if (canceled) {
      SyncResult.BazelSync(start, finish, outcome = "cancelled")
    } else {
      val moduleCount = ReadAction.compute<Int, RuntimeException> {
        ModuleManager.getInstance(project).modules.size
      }
      SyncResult.BazelSync(start, finish, outcome = observedOutcome ?: "finished", moduleCount = moduleCount)
    }

    thisLogger().info("Bazel sync ${result.resultName} in ${result.totalDuration}ms")
    project.analyticsService.recordSyncEvent(result)
  }

  // Explicit no-op overrides (instead of relying on the interface's default implementations)
  // so this class has no binary dependency on DefaultImpls, which may not exist in other
  // versions of the Bazel plugin.
  override fun allTasksCancelled() = Unit

  override fun targetUtilAvailable() = Unit
}
