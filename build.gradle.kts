import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.ksp)
  alias(libs.plugins.moshix)
  alias(libs.plugins.intelliJPlatform)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.wire) apply false
}

val pluginGroup = providers.gradleProperty("pluginGroup").get()

group = pluginGroup
// IJ_PLUGIN_VERSION env var available in CI
version = providers.environmentVariable("IJ_PLUGIN_VERSION").getOrElse("unknown")

val pluginName = providers.gradleProperty("pluginName").get()
val sinceBuildMajorVersion = "251" // corresponds to 2025.1.x versions
val sinceIdeVersionForVerification = "252.23892.409" // corresponds to the 2025.2 version
val untilIdeVersion = providers.gradleProperty("IIC.release.version").get()
val untilBuildMajorVersion = untilIdeVersion.substringBefore('.')

val javaVersion = JavaLanguageVersion.of(libs.versions.java.get()).toString()

tasks.withType<JavaCompile>().configureEach {
  options.release = javaVersion.toInt()
}

kotlin {
  jvmToolchain(javaVersion.toInt())
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
    intellijIdeaCommunity("2025.2.1")
//    androidStudio("2024.3.1.13")

    bundledPlugin("com.intellij.gradle")

    pluginVerifier()
    zipSigner()
    testFramework(TestFrameworkType.Platform)
  }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
  projectName = project.name

  pluginConfiguration {
    id = pluginGroup // matches src/main/resources/META-INF/plugin.xml => idea-plugin.id
    name = pluginName
    version = project.version.toString()
    description = "Sends basic IDE performance telemetry to analytics backend"
    vendor {
      name = "Block"
      url = "https://block.xyz/"
    }
    ideaVersion {
      sinceBuild = sinceBuildMajorVersion
      untilBuild = "$untilBuildMajorVersion.*"
    }
  }
  pluginVerification {
    ides {
      recommended()
      select {
        types = listOf(
          IntelliJPlatformType.IntellijIdeaCommunity,
          IntelliJPlatformType.IntellijIdeaUltimate,
          IntelliJPlatformType.AndroidStudio,
        )
        sinceBuild = sinceIdeVersionForVerification
        untilBuild = untilIdeVersion
      }
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

tasks {
  buildPlugin {
    archiveBaseName = pluginName
  }

  check {
    dependsOn("verifyPlugin")
  }

  patchPluginXml {
    version = version
  }

  publishPlugin {
    token = providers.environmentVariable("JETBRAINS_TOKEN") // JETBRAINS_TOKEN env var available in CI
  }

  // We need the root project to have this task so we can run `./gradlew pTML` to publish all local artifacts.
  register("publishToMavenLocal")
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
