import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration.PublishingContext

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
    // For open source projects, publishing build scans should be opt-in.
    publishing.onlyIf(ShouldPublish(providers, logger))

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

class ShouldPublish(
  private val providers: ProviderFactory,
  private val logger: Logger,
) : Spec<PublishingContext> {

  private companion object {
    const val PROP = "xyz.block.telemetry.scans.publish"
  }

  override fun isSatisfiedBy(element: PublishingContext): Boolean {
    val shouldPublish = providers.gradleProperty(PROP)
      .map { it.toBoolean() }
      .getOrElse(false)

    if (!shouldPublish) {
      logger.lifecycle("To publish build scans, opt-in by setting the Gradle property '$PROP=true'")
    }

    return shouldPublish
  }
}
