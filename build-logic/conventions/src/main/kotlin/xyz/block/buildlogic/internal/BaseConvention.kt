package xyz.block.buildlogic.internal

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal class BaseConvention(private val project: Project) {

  private lateinit var publishing: PublishingExtension
  private lateinit var catalog: BuildLogicCatalog

  fun configure(): Unit = project.run {
    pluginManager.run {
      apply("org.jetbrains.kotlin.jvm")
      apply("com.vanniktech.maven.publish.base")
      apply("com.autonomousapps.dependency-analysis")
    }

    publishing = extensions.getByType(PublishingExtension::class.java)
    catalog = BuildLogicCatalog(this)

    configureCompilationTasks()
    configurePublishing()
    configureReproducibleArchives()
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

  private fun Project.configurePublishing() {
    val publishVersion = providers.gradleProperty("publish_version").getOrElse("unversioned")

    group = "xyz.block.idea.telemetry"
    version = publishVersion

    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
      coordinates(version = publishVersion)

      pom { pom ->
        pom.name.set(name)
        pom.description.set("Links Gradle build scan telemetry with IDE telemetry")
        pom.url.set("https://github.com/block/ide-metrics-plugin")
        pom.licenses {
          it.license { l ->
            l.name.set("The Apache License, Version 2.0")
            l.url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        pom.developers { devs ->
          devs.developer { d ->
            d.id.set("joshfriend")
            d.name.set("Josh Friend")
          }
          devs.developer { d ->
            d.id.set("autonomousapps")
            d.name.set("Tony Robalik")
          }
        }
        pom.scm {
          it.connection.set("scm:git:git://github.com/block/ide-metrics-plugin.git")
          it.developerConnection.set("scm:git:ssh://github.com/block/ide-metrics-plugin.git")
          it.url.set("https://github.com/block/ide-metrics-plugin")
        }
      }
    }
  }

  private fun Project.configureReproducibleArchives() {
    tasks.withType(AbstractArchiveTask::class.java).configureEach { t ->
      t.isPreserveFileTimestamps = false
      t.isReproducibleFileOrder = true
    }
  }
}
