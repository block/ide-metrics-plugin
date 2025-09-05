pluginManagement {
  includeBuild("build-logic/conventions")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "4.1.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
  buildScan {
    val scanPublishingProperty = "xyz.block.telemetry.scans.publish"
    val shouldPublish = providers.gradleProperty(scanPublishingProperty)
      .map { it.toBoolean() }
      .getOrElse(false)

    // For open source projects, publishing build scans should be opt-in.
    publishing.onlyIf {
      if (shouldPublish) {
        true
      } else {
        logger.lifecycle("To publish build scans, opt-in by setting the gradle property '$scanPublishingProperty=true'")
        false
      }
    }

    termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
    termsOfUseAgree = "yes"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()

    // The `:gradle-plugin` module has a dependency on Develocity
    exclusiveContent {
      filter {
        includeModule("com.gradle", "develocity-gradle-plugin")
      }
      forRepository {
        gradlePluginPortal()
      }
    }
  }
}

rootProject.name = "ide-metrics-plugin"

include(":common")
include(":gradle-plugin")
include(":kotlin-eventstream2:client")
include(":kotlin-eventstream2:protos")
