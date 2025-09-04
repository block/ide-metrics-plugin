package xyz.block.idea.telemetry.util.intellij

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.gradle.model.data.GradleProjectBuildScriptData
import org.jetbrains.plugins.gradle.util.GradleConstants
import xyz.block.idea.telemetry.common.Constants
import java.util.*

private val BUILD_TRACE_ID_KEY: Key<UUID> = Key.create(Constants.BUILD_TRACE_ID_PROPERTY)

/**
 * A unique [UUID] is injected into each IDE sync invocation via a system property. These can be
 * read by build-logic and attached to a build scan to provide a correlation identifier between the
 * two telemetry systems.
 */
var Project.buildTraceId: UUID?
  get() = getUserData(BUILD_TRACE_ID_KEY)
  set(value) = putUserData(BUILD_TRACE_ID_KEY, value)

val Project.gradleProjectNode: DataNode<ProjectData>?
  get() = basePath?.let {
    ExternalSystemApiUtil.findProjectNode(this, GradleConstants.SYSTEM_ID, it)
  }

val DataNode<ProjectData>.modules: Collection<DataNode<ModuleData>>
  get() = ExternalSystemApiUtil.findAllRecursively(this, ProjectKeys.MODULE)

val DataNode<ModuleData>.buildScriptData: GradleProjectBuildScriptData?
  get() = ExternalSystemApiUtil.find(this, GradleProjectBuildScriptData.KEY)?.data
