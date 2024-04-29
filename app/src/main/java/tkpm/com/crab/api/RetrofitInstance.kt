package tkpm.com.crab.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tkpm.com.crab.BuildConfig

class RetrofitInstance {
    fun get(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}