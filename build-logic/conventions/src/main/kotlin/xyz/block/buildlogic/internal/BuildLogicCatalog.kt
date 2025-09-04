package xyz.block.buildlogic.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion

internal class BuildLogicCatalog(private val project: Project) {

  val versionCatalog: VersionCatalog by unsafeLazy {
    project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  }

  /*
   * Versions
   */

  val javaVersion: JavaLanguageVersion by unsafeLazy { JavaLanguageVersion.of(getVersion("java")) }

  private fun getVersion(name: String): String {
    return versionCatalog
      .findVersion(name)
      .orElseThrow {
        NoSuchElementException(
          "No version named '$name' found in version catalog '${versionCatalog.name}'. Check spelling and case."
        )
      }
      .requiredVersion
  }

  /*
   * Libraries
   *
   * nb: the below was copy-pasted and none of these necessarily exist. They're left as examples.
   */

  val assertj: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("assertj").orElseThrow()
  }

  val gradleTestKitSupport: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("gradleTestKitSupport").orElseThrow()
  }

  val junitParams: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("junitParams").orElseThrow()
  }

  val junitApi: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("junitApi").orElseThrow()
  }

  val junitBom: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("junitBom").orElseThrow()
  }

  val junitEngine: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("junitEngine").orElseThrow()
  }

  val junitPlatformLauncher: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("junitPlatformLauncher").orElseThrow()
  }

  val kotlinGradleBom: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("kotlinGradleBom").orElseThrow()
  }

  val kotlinStdLib: Provider<MinimalExternalModuleDependency> by unsafeLazy {
    versionCatalog.findLibrary("kotlinStdLib").orElseThrow()
  }

  fun getLibrary(name: String): MinimalExternalModuleDependency {
    return versionCatalog
      .findLibrary(name)
      .orElseThrow { IllegalArgumentException("No entry for '$name' found in version catalog!") }
      .get()
  }
}
