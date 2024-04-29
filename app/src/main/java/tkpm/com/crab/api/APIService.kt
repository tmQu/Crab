package tkpm.com.crab.api

import android.util.Log
import tkpm.com.crab.objects.BaseResponse
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response

class APIService {
    val retrofitClient = RetrofitInstance().get()

    inline fun <reified T> doGet(endpoint: String, callback: APICallback<Any>) {
        val service = retrofitClient.create(APIInterface::class.java).get(endpoint)

        // Enqueue the request
        service.enqueue(object : retrofit2.Callback<BaseResponse<JsonElement>> {
            override fun onResponse(
                call: Call<BaseResponse<JsonElement>>,
                response: Response<BaseResponse<JsonElement>>
            ) {
                if (response.isSuccessful && response.body()!!.success) {
                    val data = response.body()?.data
                    val gson = Gson()
                    val type = object : TypeToken<T>() {}.type
                    val result = gson.fromJson(data, type) as Any

                    // Custom callback to process data
                    callback.onSuccess(result)
                } else {
                    callback.onError(Throwable(response.body()!!.message))
                }
            }

            override fun onFailure(call: Call<BaseResponse<JsonElement>>, t: Throwable) {
                // Custom callback to process data
                callback.onError(t)
            }
        })
    }

    inline fun <reified T> doPatch(endpoint: String, body: Any, callback: APICallback<Any>) {
        val service = retrofitClient.create(APIInterface::class.java).patch(endpoint, body)

        // Enqueue the request
        service.enqueue(object : retrofit2.Callback<BaseResponse<JsonElement>> {
            override fun onResponse(
                call: Call<BaseResponse<JsonElement>>,
                response: Response<BaseResponse<JsonElement>>
            ) {
                if (response.isSuccessful && response.body()!!.success) {
                    val data = response.body()?.data
                    Log.d("API_SERVICE", "Data: $data")
                    val gson = Gson()
                    val type = object : TypeToken<T>() {}.type
                    val result = gson.fromJson(data, type) as Any

                    // Custom callback to process data
                    callback.onSuccess(result)
                } else {
                    callback.onError(Throwable(response.body()!!.message))
                }
            }

            override fun onFailure(call: Call<BaseResponse<JsonElement>>, t: Throwable) {
                // Custom callback to process data
                callback.onError(t)
            }
        })
    }

    inline fun <reified T> doPutMultipart(endpoint: String, body: MultipartBody.Part, callback: APICallback<Any>) {
        val service = retrofitClient.create(APIInterface::class.java).putMultipart(endpoint, body)

        // Enqueue the request
        service.enqueue(object : retrofit2.Callback<BaseResponse<JsonElement>> {
            override fun onResponse(
                call: Call<BaseResponse<JsonElement>>,
                response: Response<BaseResponse<JsonElement>>
            ) {
                if (response.isSuccessful && response.body()!!.success) {
                    val data = response.body()?.data
                    val gson = Gson()
                    val type = object : TypeToken<T>() {}.type
                    val result = gson.fromJson(data, type) as Any

                    // Custom callback to process data
                    callback.onSuccess(result)
                } else {
                    callback.onError(Throwable(response.body()!!.message))
                }
            }

            override fun onFailure(call: Call<BaseResponse<JsonElement>>, t: Throwable) {
                // Custom callback to process data
                callback.onError(t)
            }
        })
    }

    inline fun <reified T> doPost(endpoint: String, body: Any, callback: APICallback<Any>) {
        val service = retrofitClient.create(APIInterface::class.java).post(endpoint, body)

        // Enqueue the request
        service.enqueue(object : retrofit2.Callback<BaseResponse<JsonElement>> {
            override fun onResponse(
                call: Call<BaseResponse<JsonElement>>,
                response: Response<BaseResponse<JsonElement>>
            ) {
                if (response.isSuccessful && response.body()!!.success) {
                    val data = response.body()?.data
                    val gson = Gson()
                    val type = object : TypeToken<T>() {}.type
                    val result = gson.fromJson(data, type) as Any

                    // Custom callback to process data
                    callback.onSuccess(result)
                } else {
                    callback.onError(Throwable(response.body()!!.message))
                }
            }

            override fun onFailure(call: Call<BaseResponse<JsonElement>>, t: Throwable) {
                // Custom callback to process data
                callback.onError(t)
            }
        })
    }

    inline fun <reified T> doDelete(endpoint: String, callback: APICallback<Any>) {
        val service = retrofitClient.create(APIInterface::class.java).delete(endpoint)

        // Enqueue the request
        service.enqueue(object : retrofit2.Callback<BaseResponse<JsonElement>> {
            override fun onResponse(
                call: Call<BaseResponse<JsonElement>>,
                response: Response<BaseResponse<JsonElement>>
            ) {
                if (response.isSuccessful && response.body()!!.success) {
                    val data = response.body()?.data
                    val gson = Gson()
                    val type = object : TypeToken<T>() {}.type
                    val result = gson.fromJson(data, type) as Any

                    // Custom callback to process data
                    callback.onSuccess(result)
                } else {
                    callback.onError(Throwable(response.body()!!.message))
                }
            }

            override fun onFailure(call: Call<BaseResponse<JsonElement>>, t: Throwable) {
                // Custom callback to process data
                callback.onError(t)
            }
        })
    }
}