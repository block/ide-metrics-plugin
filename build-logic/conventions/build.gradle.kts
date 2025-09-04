plugins {
  id("java-gradle-plugin")
  alias(libs.plugins.kotlin)
}

group = "xyz.block.build-logic"
version = "1"

gradlePlugin {
  plugins {
    create("library") {
      id = "build-logic.lib"
      implementationClass = "xyz.block.buildlogic.LibraryConvention"
    }
    create("plugin") {
      id = "build-logic.plugin"
      implementationClass = "xyz.block.buildlogic.PluginConvention"
    }
    // TODO
    // create("idea") {
    //   id = "build-logic.idea"
    //   implementationClass = "xyz.block.buildlogic.IdeaConvention"
    // }
    // create("root") {
    //   id = "build-logic.root"
    //   implementationClass = "xyz.block.buildlogic.RootConvention"
    // }
  }
}

// https://github.com/gradle/gradle/issues/22600
tasks.withType<ValidatePlugins>().configureEach {
  enableStricterValidation = true
}

val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val javaVersion = JavaLanguageVersion.of(
  versionCatalog.findVersion("java").orElseThrow().requiredVersion
).toString()

tasks.withType<JavaCompile>().configureEach {
  options.release = javaVersion.toInt()
}

kotlin {
  explicitApi()

  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersion))
    freeCompilerArgs.add("-Xjdk-release=$javaVersion")
  }
}

dependencies {
  implementation(platform(libs.kotlinGradleBom))

  implementation(libs.dependencyAnalysisPlugin)
  implementation(libs.kotlinGradlePlugin)
  implementation(libs.mavenPublishPlugin)
}
