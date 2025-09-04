package xyz.block.buildlogic

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
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
    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      configure(
        KotlinJvm(
          javadocJar = JavadocJar.Empty(),
          sourcesJar = true,
        )
      )
    }
  }
}