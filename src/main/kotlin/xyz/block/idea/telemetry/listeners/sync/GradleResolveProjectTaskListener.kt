@file:Suppress("UnstableApiUsage")

package xyz.block.idea.telemetry.listeners.sync

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import xyz.block.idea.telemetry.services.SyncState.Companion.syncState
import xyz.block.idea.telemetry.util.intellij.buildScriptData
import xyz.block.idea.telemetry.util.intellij.gradleProjectNode
import xyz.block.idea.telemetry.util.intellij.isGradleResolveProjectTask
import xyz.block.idea.telemetry.util.intellij.modules

/**
 * Listen for state of Gradle sync task.
 */
class GradleResolveProjectTaskListener : ExternalSystemTaskNotificationListener, BuildProgressListener {
  override fun onStart(projectPath: String, id: ExternalSystemTaskId) {
    if (!id.isGradleResolveProjectTask) return
    val project = id.findProject() ?: return
    project.syncState.syncStarted()
  }

  override fun onSuccess(projectPath: String, id: ExternalSystemTaskId) {
    if (!id.isGradleResolveProjectTask) return
    val project = id.findProject() ?: return

    val projectsWithBuildFiles = project.gradleProjectNode?.modules?.filter { module ->
      module.buildScriptData?.buildScriptSource?.exists() == true
    }

    project.syncState.syncGradleFinished(projectsWithBuildFiles?.size ?: -1)
  }

  override fun onFailure(projectPath: String, id: ExternalSystemTaskId, e: Exception) {
    if (!id.isGradleResolveProjectTask) return
    val project = id.findProject() ?: return
    project.syncState.syncError(e)
  }

  override fun onCancel(projectPath: String, id: ExternalSystemTaskId) {
    if (!id.isGradleResolveProjectTask) return
    val project = id.findProject() ?: return
    project.syncState.syncCanceled()
  }

  override fun onEvent(id: Any, event: BuildEvent) = Unit
  override fun onEnd(projectPath: String, id: ExternalSystemTaskId) = Unit
  override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) = Unit
  override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) = Unit
  override fun beforeCancel(id: ExternalSystemTaskId) = Unit
}

