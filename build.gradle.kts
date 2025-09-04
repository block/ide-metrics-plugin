import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.ksp)
  alias(libs.plugins.moshix)
  alias(libs.plugins.intelliJPlatform)
  alias(libs.plugins.changelog)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.wire) apply false
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val javaVersion = JavaLanguageVersion.of(
  versionCatalog.findVersion("java").orElseThrow().requiredVersion
).toString()

tasks.withType<JavaCompile>().configureEach {
  options.release = javaVersion.toInt()
}

kotlin {
  jvmToolchain(javaVersion.toInt())

  // TODO(tsr): confirm with Josh
  // compilerOptions {
  //   jvmTarget.set(JvmTarget.fromTarget(javaVersion))
  //   freeCompilerArgs.add(
  //     // https://youtrack.jetbrains.com/issue/KT-49746/Support-Xjdk-release-in-gradle-toolchain#focus=Comments-27-12547382.0-0
  //     "-Xjdk-release=$javaVersion",
  //   )
  // }
}

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  implementation(project(":common"))
  implementation(project(":kotlin-eventstream2:client"))

  intellijPlatform {
    intellijIdeaCommunity("2025.1.2")
//    androidStudio("2024.3.1.13")

    bundledPlugin("com.intellij.gradle")

    pluginVerifier()
    zipSigner()
    testFramework(TestFrameworkType.Platform)
  }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
  pluginConfiguration {
    version = providers.gradleProperty("pluginVersion")

    // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
    description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
      val start = "<!-- Plugin description -->"
      val end = "<!-- Plugin description end -->"

      with(it.lines()) {
        if (!containsAll(listOf(start, end))) {
          throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
        }
        subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
      }
    }

    val changelog = project.changelog // local variable for configuration cache compatibility
    // Get the latest available change notes from the changelog file
    changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
      with(changelog) {
        renderItem(
          (getOrNull(pluginVersion) ?: getUnreleased())
            .withHeader(false)
            .withEmptySections(false),
          Changelog.OutputType.HTML,
        )
      }
    }

    ideaVersion {
      sinceBuild = "251"
    }
  }

  signing {
    certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
    privateKey = providers.environmentVariable("PRIVATE_KEY")
    password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
  }

  publishing {
    token = providers.environmentVariable("PUBLISH_TOKEN")
    // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    channels = providers.gradleProperty("pluginVersion")
      .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
  }

  pluginVerification {
    ides {
      recommended()
    }
  }
}

intellijPlatformTesting {
  runIde {
    register("runIdeForUiTests") {
      task {
        jvmArgumentProviders += CommandLineArgumentProvider {
          listOf(
            "-Drobot-server.port=8082",
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false",
          )
        }
      }

      plugins {
        robotServerPlugin()
      }
    }
  }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  groups.empty()
  repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
  wrapper {
    gradleVersion = providers.gradleProperty("gradleVersion").get()
  }

  publishPlugin {
    dependsOn(patchChangelog)
  }

  // Convenience tasks
  register("publishToMavenLocal") {
    dependsOn(":common:publishToMavenLocal", ":gradle-plugin:publishToMavenLocal")
  }
}

dependencyAnalysis {
  reporting {
    printBuildHealth(true)
  }
  abi {
    exclusions {
      // This is an IDE plugin, not a library. It doesn't have a public API.
      excludeSourceSets("main")
    }
  }
  issues {
    all {
      onAny {
        severity("fail")
      }
      onUnusedDependencies {
        exclude(
          // A plugin is adding this
          "com.squareup.moshi:moshi",
        )
      }
    }
  }
}
