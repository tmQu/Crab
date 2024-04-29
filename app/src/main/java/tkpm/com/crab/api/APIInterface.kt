package tkpm.com.crab.api

import tkpm.com.crab.objects.BaseResponse
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface APIInterface {
    @GET("{endpoint}")
    fun get(@Path("endpoint", encoded = true) endpoint: String): Call<BaseResponse<JsonElement>>

    @PATCH("{endpoint}")
    fun patch(@Path("endpoint", encoded = true) endpoint: String, @Body body: Any): Call<BaseResponse<JsonElement>>

    @Multipart
    @PUT("{endpoint}")
    fun putMultipart(@Path("endpoint", encoded = true) endpoint: String, @Part body: MultipartBody.Part): Call<BaseResponse<JsonElement>>

    @POST("{endpoint}")
    fun post(@Path("endpoint", encoded = true) endpoint: String, @Body body: Any): Call<BaseResponse<JsonElement>>

    @DELETE("{endpoint}")
    fun delete(@Path("endpoint", encoded = true) endpoint: String): Call<BaseResponse<JsonElement>>
}
