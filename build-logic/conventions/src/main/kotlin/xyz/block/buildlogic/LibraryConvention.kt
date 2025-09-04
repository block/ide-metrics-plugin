package xyz.block.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import xyz.block.buildlogic.internal.BaseConvention

/**
 * A convention plugin for library projects in this repo.
 *
 * ```
 * plugins {
 *   id("build-logic.lib")
 * }
 * ```
 */
public abstract class LibraryConvention : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    BaseConvention(this).configure()

    configurePublishing()
  }

  private fun Project.configurePublishing() {
    extensions.getByType(PublishingExtension::class.java).publications { container ->
      container.create("library", MavenPublication::class.java) { pub ->
        pub.from(components.getAt("java"))
      }
    }
  }
}