package com.squareup.http

import com.squareup.eventstream.EventstreamService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import retrofit2.create
import java.net.Proxy
import java.util.concurrent.TimeUnit.SECONDS

public object Endpoint {
  public fun create(baseUrl: String): EventstreamService {
    return createRetrofitInstance(baseUrl, false).create()
  }

  private fun createRetrofitInstance(
    baseUrl: String,
    disableProxy: Boolean
  ): Retrofit {
    val httpClient =
      OkHttpClient.Builder()
        .connectTimeout(30, SECONDS)
        .readTimeout(30, SECONDS)
        .writeTimeout(30, SECONDS)
        .apply {
          if (disableProxy) {
            proxy(Proxy.NO_PROXY)
          }
        }
        .build()

    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(httpClient)
      .addConverterFactory(WireConverterFactory.create())
      .build()
  }
}
