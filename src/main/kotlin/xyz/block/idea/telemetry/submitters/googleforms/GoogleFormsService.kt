package xyz.block.idea.telemetry.submitters.googleforms

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

internal interface GoogleFormsService {
  @GET
  fun submitForm(@Url url: String): Call<ResponseBody>

  companion object {
    operator fun invoke(): GoogleFormsService =
      GoogleFormsEndpoint.create()
  }
}
