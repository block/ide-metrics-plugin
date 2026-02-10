package xyz.block.idea.telemetry.submitters.googleforms

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import java.util.concurrent.TimeUnit

internal object GoogleFormsEndpoint {

  fun create(): GoogleFormsService =
    createRetrofitInstance().create()

  private fun createRetrofitInstance(): Retrofit {
    val httpClient = OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()

    return Retrofit.Builder()
      .baseUrl("https://docs.google.com/forms/")
      .client(httpClient)
      .build()
  }
}
