package xyz.block.idea.telemetry.listeners.sync

import com.intellij.build.BuildProgressListener
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FailureResult
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.SkippedResult
import com.intellij.build.events.SuccessResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bsp.protocol.TaskId

/**
 * Observes the build events the Bazel plugin publishes to the Sync view to determine the
 * outcome of a Bazel sync.
 *
 * The Bazel plugin's public [org.jetbrains.bazel.sync.status.SyncStatusListener] API only
 * reports finished/cancelled, but the plugin finishes its sync build in the Sync view with a
 * typed result (failure/success/skipped).
 *
 * How the plugin reports its sync build is an implementation detail, not a stable API, so
 * [consumeOutcome] may return null if the plugin's behavior changes; callers should fall back
 * to a generic outcome.
 */
@Service(Service.Level.PROJECT)
internal class BazelSyncOutcomeTracker(project: Project) : BuildProgressListener, Disposable {

  private val lock = Any()
  private var lastOutcome: String? = null

  init {
    project.getService(SyncViewManager::class.java).addListener(this, this)
  }

  /** Called when a Bazel sync starts. Clears any outcome left over from a previous sync. */
  fun syncWindowStarted() {
    synchronized(lock) {
      lastOutcome = null
    }
  }

  /**
   * Returns the outcome ("succeeded", "partially_succeeded", "failed", or "cancelled") of the
   * Bazel sync build observed since [syncWindowStarted], or null if none was observed.
   */
  fun consumeOutcome(): String? = synchronized(lock) {
    val outcome = lastOutcome
    lastOutcome = null
    outcome
  }

  override fun onEvent(buildId: Any, event: BuildEvent) {
    if (event !is FinishBuildEvent || !isBazelSyncBuild(buildId)) return
    val outcome = when (val result = event.result) {
      is FailureResult -> "failed"
      is SkippedResult -> "cancelled"
      // The Bazel plugin reports partial success (some targets failed to import) as a
      // success result with isUpToDate = true; a plain success has isUpToDate = false.
      is SuccessResult -> if (result.isUpToDate) "partially_succeeded" else "succeeded"
      else -> null
    }
    thisLogger().info("Bazel sync build finished with result ${event.result.javaClass.simpleName} -> $outcome")
    synchronized(lock) { lastOutcome = outcome }
  }

  /**
   * Matching on the build id works because the tracker only listens to [SyncViewManager]
   * (not the Build view), and the Bazel plugin is the only producer using this task id there.
   */
  private fun isBazelSyncBuild(buildId: Any): Boolean =
    when (buildId) {
      // 2025.2.x passes the sync task id as a plain String.
      is String -> buildId == BAZEL_SYNC_TASK_ID
      // Newer versions pass a TaskId; its `id` property is the same constant. The getter is
      // binary-compatible across the TaskId shape change upstream.
      is TaskId -> buildId.id == BAZEL_SYNC_TASK_ID
      else -> false
    }

  override fun dispose() = Unit

  companion object {
    private const val BAZEL_SYNC_TASK_ID = "project-sync"
  }
}

/**
 * Instantiates [BazelSyncOutcomeTracker] when a project opens so its Sync view listener is
 * registered before the Bazel plugin starts its first sync.
 */
internal class BazelSyncOutcomeTrackerInitializer : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.getService(BazelSyncOutcomeTracker::class.java)
  }
}
