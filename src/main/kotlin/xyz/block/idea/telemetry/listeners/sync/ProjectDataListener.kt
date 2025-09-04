package xyz.block.idea.telemetry.listeners.sync

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import xyz.block.idea.telemetry.services.SyncState.Companion.syncState

/**
 * Listen for progress on the IDE half of project sync.
 */
class ProjectDataListener(private val project: Project) : ProjectDataImportListener {
  override fun onImportFinished(projectPath: String?) {
    project.syncState.syncFinished()
  }

  override fun onImportFailed(projectPath: String?, t: Throwable) {
    // If `onImportFailed` is called because of `ProcessCanceledException`, it results in `isCancelled == true`, and this is the way
    // we detect this case since we don't have access to the exception instance itself here.
    if (ProgressManager.getGlobalProgressIndicator()?.isCanceled == true) {
      // sync was cancelled
      thisLogger().warn("Project data import $projectPath has been cancelled")
      project.syncState.syncCanceled()
    } else {
      thisLogger().error("Project data import $projectPath has failed", t)
      project.syncState.syncError(t)
    }
  }
}