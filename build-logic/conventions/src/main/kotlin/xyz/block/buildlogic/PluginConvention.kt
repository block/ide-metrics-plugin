package xyz.block.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugin.devel.tasks.ValidatePlugins
import xyz.block.buildlogic.internal.BaseConvention
import xyz.block.buildlogic.internal.BuildLogicCatalog

/**
 * A convention plugin for plugin projects in this repo.
 *
 * ```
 * plugins {
 *   id("build-logic.plugin")
 * }
 * ```
 */
public abstract class PluginConvention : Plugin<Project> {

  private lateinit var catalog: BuildLogicCatalog

  override fun apply(target: Project): Unit = target.run {
    pluginManager.apply("java-gradle-plugin")
    BaseConvention(this).configure()

    catalog = BuildLogicCatalog(this)

    configureValidation()
  }

  /**
   * @see <a href="https://github.com/gradle/gradle/issues/22600">Enable stricter validation of plugins by default for
   *   validatePlugins task</a>
   */
  private fun Project.configureValidation() {
    tasks.withType(ValidatePlugins::class.java).configureEach { t ->
      t.enableStricterValidation.set(true)
    }
  }
}
