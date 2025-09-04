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
    // TODO(tsr): re-enable later
    publishing.onlyIf { false }

    termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
    termsOfUseAgree = "yes"

    capture {
      fileFingerprints = true
    }
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
