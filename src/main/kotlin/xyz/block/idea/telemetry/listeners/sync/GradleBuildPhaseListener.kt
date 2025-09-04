@file:Suppress("UnstableApiUsage")

package xyz.block.idea.telemetry.listeners.sync

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import org.gradle.internal.operations.BuildOperationCategory
import org.gradle.tooling.LongRunningOperation
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.OperationType.BUILD_PHASE
import org.gradle.tooling.events.lifecycle.BuildPhaseFinishEvent
import org.gradle.tooling.events.lifecycle.BuildPhaseProgressEvent
import org.gradle.tooling.events.lifecycle.BuildPhaseStartEvent
import org.gradle.tooling.model.build.BuildEnvironment
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.project.GradleExecutionHelperExtension
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import xyz.block.idea.telemetry.services.SyncState.Companion.syncState
import xyz.block.idea.telemetry.util.gradle.addProgressListener
import xyz.block.idea.telemetry.util.gradle.withBuildTraceId
import xyz.block.idea.telemetry.util.intellij.buildTraceId
import java.util.*

/**
 * A listener that uses the Gradle tooling API to track [BUILD_PHASE] events. Also injects
 * [BUILD_TRACE_ID_PROPERTY][xyz.block.idea.telemetry.common.Constants.BUILD_TRACE_ID_PROPERTY] into the build.
 */
class GradleBuildPhaseListener : GradleExecutionHelperExtension {
  override fun prepareForExecution(
    id: ExternalSystemTaskId,
    operation: LongRunningOperation,
    settings: GradleExecutionSettings,
    buildEnvironment: BuildEnvironment?
  ) {
    val project = id.findProject() ?: return
    if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
      prepareForSync(project, operation, buildEnvironment)
    }
  }

  private fun prepareForSync(
    project: Project,
    operation: LongRunningOperation,
    buildEnvironment: BuildEnvironment?,
  ) {
    val id = UUID.randomUUID()
    project.buildTraceId = id

    val gradleVersion = buildEnvironment?.gradle?.gradleVersion
      ?.let { GradleVersion.version(it) }

    operation
      .withBuildTraceId(id)
      .addProgressListener(BUILD_PHASE) { event ->
        if (project.isDisposed) return@addProgressListener
        if (event !is BuildPhaseProgressEvent) return@addProgressListener
        val phase = BuildOperationCategory.valueOf(event.descriptor.buildPhase)

        when (event) {
          is BuildPhaseStartEvent -> {
            when (phase) {
              BuildOperationCategory.CONFIGURE_ROOT_BUILD -> project.syncState.syncConfigureIncludedBuildsFinished()
              BuildOperationCategory.CONFIGURE_BUILD -> project.syncState.hasIncludedBuilds()
              else -> { /* ignored */ }
            }
          }
          is BuildPhaseFinishEvent -> {
            val succeeded = event !is FailureResult
            if (succeeded && phase == BuildOperationCategory.CONFIGURE_ROOT_BUILD) {
              project.syncState.syncConfigureRootBuildFinished(gradleVersion)
            }
          }
        }
      }
  }
}