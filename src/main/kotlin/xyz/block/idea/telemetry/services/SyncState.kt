@file:OptIn(ExperimentalStdlibApi::class)

package xyz.block.idea.telemetry.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.gradle.util.GradleVersion
import xyz.block.idea.telemetry.events.SyncPhase
import xyz.block.idea.telemetry.events.SyncResult
import xyz.block.idea.telemetry.services.Analytics.Companion.analyticsService
import xyz.block.idea.telemetry.util.intellij.buildTraceId
import xyz.block.idea.telemetry.util.now
import java.util.*

internal class SyncState(private val project: Project) {
  companion object {
    @JvmStatic
    fun get(project: Project): SyncState = project.getService(SyncState::class.java)

    val Project.syncState: SyncState get() = get(this)
  }

  private var startTimestamp: Long = -1
  private var hasIncludedBuilds: Boolean = false
  private var finishConfigureIncludedBuildsTimestamp: Long = -1
  private var finishConfigureRootBuildTimestamp: Long = -1
  private var gradleFinishTimestamp: Long = -1
  private var gradleVersion: GradleVersion? = null
  private var gradleProjectCount: Int = -1
  private var finishTimestamp: Long = -1
  private var phases: LinkedList<SyncPhase> = LinkedList(SyncPhase.entries)

  private fun reset() {
    startTimestamp = -1
    hasIncludedBuilds = false
    finishConfigureIncludedBuildsTimestamp = -1
    finishConfigureRootBuildTimestamp = -1
    gradleFinishTimestamp = -1
    gradleVersion = null
    gradleProjectCount = -1
    finishTimestamp = -1
    phases = LinkedList(SyncPhase.entries)
  }

  private fun logEvent(result: SyncResult) {
    when (result) {
      is SyncResult.SyncSucceeded -> thisLogger().run {
        info("Sync ${project.buildTraceId} with $gradleVersion succeeded for ${project.name} (${gradleProjectCount} projects)")
        if (hasIncludedBuilds) info("Configure included builds duration: ${result.configureIncludedBuildsDuration}")
        info("Configure root project duration: ${result.configureRootProjectDuration}")
        info("Gradle execution duration: ${result.gradleExecutionDuration}")
        info("Total Gradle duration: ${result.gradleDuration}")
        info("IDE duration: ${result.ideDuration}")
        info("Total Duration: ${result.totalDuration}")
      }
      is SyncResult.SyncCancelled -> thisLogger().info("sync cancelled at ${result.phase}, Total Duration: ${result.totalDuration}ms")
      is SyncResult.SyncFailed -> thisLogger().info("sync failed at ${result.phase}, Total Duration: ${result.totalDuration}ms - ${result.exception.message}")
    }
    project.analyticsService.recordSyncEvent(result)
    reset()
  }

  fun syncStarted() {
    thisLogger().info("syncStarted")
    reset()
    phases.pop()
    startTimestamp = now()
  }

  fun hasIncludedBuilds() {
    thisLogger().info("hasIncludedBuilds")
    hasIncludedBuilds = true
  }

  fun syncConfigureIncludedBuildsFinished() {
    thisLogger().info("syncConfigureIncludedBuildsFinished")
    phases.pop()
    finishConfigureIncludedBuildsTimestamp = now()
  }

  fun syncConfigureRootBuildFinished(gradleVersion: GradleVersion?) {
    thisLogger().info("syncConfigureRootBuildFinished ($gradleVersion)")
    if (!hasIncludedBuilds) {
      phases.pop()
    }
    phases.pop()
    this.gradleVersion = gradleVersion
    finishConfigureRootBuildTimestamp = now()
  }

  fun syncGradleFinished(projectCount: Int) {
    thisLogger().info("syncGradleFinished")
    phases.pop()
    gradleFinishTimestamp = now()
    gradleProjectCount = projectCount
  }

  fun syncFinished() {
    thisLogger().info("syncFinished")
    finishTimestamp = now()
    logEvent(
      SyncResult.SyncSucceeded(
        project.buildTraceId,
        gradleVersion,
        startTimestamp,
        finishTimestamp,
        gradleProjectCount,
        hasIncludedBuilds,
        finishConfigureIncludedBuildsTimestamp,
        finishConfigureRootBuildTimestamp,
        gradleFinishTimestamp
      )
    )
  }

  fun syncCanceled() {
    thisLogger().info("syncCanceled")
    finishTimestamp = now()
    logEvent(SyncResult.SyncCancelled(
      project.buildTraceId,
      gradleVersion,
      startTimestamp,
      finishTimestamp,
      phases.pop()
    ))
  }

  fun syncError(error: Throwable) {
    thisLogger().error("syncError", error)
    finishTimestamp = now()
    logEvent(SyncResult.SyncFailed(
      project.buildTraceId,
      gradleVersion,
      startTimestamp,
      finishTimestamp,
      phases.pop(),
      error
    ))
  }
}