package xyz.block.idea.telemetry.util.intellij

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Indicates if the task ID is for Gradle sync.
 */
internal val ExternalSystemTaskId.isGradleResolveProjectTask: Boolean
  get() = projectSystemId == GradleConstants.SYSTEM_ID && type == ExternalSystemTaskType.RESOLVE_PROJECT
