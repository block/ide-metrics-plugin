// Runs inside the Gradle daemon during IDE sync, injected by IsolatedProjectsResolverExtension.
// Keep this module Java-only with no runtime dependencies so nothing leaks onto the build's classpath.
plugins {
  `java-library`
  // Version comes from the root project, which applies org.jetbrains.intellij.platform.
  id("org.jetbrains.intellij.platform.module")
}

val javaVersion = JavaLanguageVersion.of(libs.versions.java.get()).toString()

tasks.withType<JavaCompile>().configureEach {
  options.release = javaVersion.toInt()
}

intellijPlatform {
  // Nothing here uses JetBrains annotations or forms, so there is nothing to instrument. It also
  // avoids instrumentCode and instrumentTestCode racing on the shared Ant builder, see
  // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2193
  instrumentCode = false
}

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  // Gradle provides javax.inject at runtime, it is only needed to compile the @Inject holder.
  compileOnly(libs.javaxInject)

  intellijPlatform {
    intellijIdeaCommunity("2025.2.3")
    // Provides org.jetbrains.plugins.gradle.tooling.ModelBuilderService and the bundled Gradle API.
    bundledPlugin("com.intellij.gradle")
  }
}
