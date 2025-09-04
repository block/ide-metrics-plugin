plugins {
  id("build-logic.lib")
  id("com.google.devtools.ksp")
}

dependencies {
  implementation(project(":kotlin-eventstream2:protos"))
  implementation(libs.kotlinStdLib)
  implementation(libs.moshi)
  implementation(libs.okhttp)
  implementation(libs.okio)
  implementation(libs.retrofit)
  implementation(libs.retrofitConvertireWire)

  ksp(libs.moshiKotlinCodegen)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinTest)
  testImplementation(libs.retrofitMock)
  testImplementation(libs.truth)
}
