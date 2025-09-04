plugins {
  id("build-logic.plugin")
}

gradlePlugin {
  plugins {
    create("plugin") {
      id = "xyz.block.ide-telemetry"
      implementationClass = "xyz.block.idea.telemetry.gradle.IdeTelemetryPlugin"
    }
  }
}

dependencies {
  implementation(project(":common"))
  implementation(libs.kotlinStdLib)

  compileOnly(libs.develocityGradlePlugin) {
    because("We don't want to enforce a Develocity version on users")
  }
}
