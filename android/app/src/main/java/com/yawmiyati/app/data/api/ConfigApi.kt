package com.yawmiyati.app.data.api

import com.yawmiyati.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class AppConfigResponse(
    val minSupportedVersionCode: Int,
    val latestVersionCode: Int,
    val apkDownloadUrl: String,
    val supportWhatsapp: String,
)

interface ConfigApiService {
    @GET("app/config/")
    suspend fun getConfig(): AppConfigResponse
}

object ConfigApi {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val service: ConfigApiService by lazy { retrofit.create(ConfigApiService::class.java) }
}
