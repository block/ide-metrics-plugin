plugins {
  id("build-logic.lib")
}

dependencies {
  api(libs.wireRuntime)

  implementation(libs.kotlinStdLib)
  implementation(libs.okio)
}
