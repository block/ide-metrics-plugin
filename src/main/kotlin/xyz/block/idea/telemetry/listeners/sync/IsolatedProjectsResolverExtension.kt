package xyz.block.idea.telemetry.listeners.sync

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import xyz.block.idea.telemetry.gradle.tooling.IsolatedProjectsModel
import xyz.block.idea.telemetry.gradle.tooling.IsolatedProjectsModelBuilder
import xyz.block.idea.telemetry.services.SyncState.Companion.syncState

/**
 * Asks the Gradle daemon whether isolated projects was active for the synced build by fetching
 * [IsolatedProjectsModel], which [IsolatedProjectsModelBuilder] builds from Gradle's own
 * `BuildFeatures` service. The `:gradle-tooling-extension` jar containing both classes is added
 * to the daemon's classpath via [getToolingExtensionsClasses].
 */
class IsolatedProjectsResolverExtension : AbstractProjectResolverExtension() {
  override fun getExtraBuildModelClasses(): Set<Class<*>> = setOf(IsolatedProjectsModel::class.java)

  override fun getToolingExtensionsClasses(): Set<Class<*>> =
    setOf(IsolatedProjectsModelBuilder::class.java, IsolatedProjectsModel::class.java)

  override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {
    val model = resolverCtx.getRootModel(IsolatedProjectsModel::class.java)
    if (model == null) {
      thisLogger().warn("IsolatedProjectsModel was not provided by Gradle")
    } else {
      resolverCtx.externalSystemTaskId.findProject()?.syncState
        ?.syncBuildFeaturesResolved(isolatedProjectsEnabled = model.isEnabled)
    }
    super.populateProjectExtraModels(gradleProject, ideProject)
  }
}
