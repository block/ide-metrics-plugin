package xyz.block.buildlogic.internal

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal class BaseConvention(private val project: Project) {

  private lateinit var publishing: PublishingExtension
  private lateinit var catalog: BuildLogicCatalog

  fun configure(): Unit = project.run {
    pluginManager.run {
      apply("org.jetbrains.kotlin.jvm")
      apply("maven-publish")
      apply("com.autonomousapps.dependency-analysis")
    }

    publishing = extensions.getByType(PublishingExtension::class.java)
    catalog = BuildLogicCatalog(this)

    configureCompilationTasks()
    configurePublishing()
  }

  private fun Project.configureCompilationTasks() {
    val catalog = BuildLogicCatalog(this)
    val javaVersion = catalog.javaVersion.toString()

    tasks.withType(JavaCompile::class.java).configureEach {
      it.options.release.set(javaVersion.toInt())
    }

    extensions.getByType(KotlinJvmProjectExtension::class.java).run {
      explicitApi()

      compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
        freeCompilerArgs.add("-Xjdk-release=$javaVersion")
      }
    }
  }

  // TODO(tsr): update for maven-publish
  private fun Project.configurePublishing() {
    group = "xyz.block.idea.telemetry"
    version = System.getProperty("publish_version", "unversioned")
  }
}
