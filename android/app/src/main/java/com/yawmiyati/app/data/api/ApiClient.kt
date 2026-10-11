package com.yawmiyati.app.data.api

import com.yawmiyati.app.BuildConfig
import com.yawmiyati.app.data.auth.TokenManager
import com.yawmiyati.app.data.auth.TokenStore
import kotlinx.coroutines.sync.Mutex
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var tokenManager: TokenManager? = null

    // Proxy so TokenAuthenticator always reads the current tokenManager value,
    // even if init() is called after the lazy chain has already been triggered.
    private val tokenStore: TokenStore = object : TokenStore {
        override fun getAccessToken() = tokenManager?.getAccessToken()
        override fun getRefreshToken() = tokenManager?.getRefreshToken()
        override fun saveTokens(access: String, refresh: String) { tokenManager?.saveTokens(access, refresh) }
        override fun clear() { tokenManager?.clear() }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val authInterceptor = Interceptor { chain ->
        val token = tokenManager?.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // Separate client used exclusively for token refresh — no authenticator to avoid recursion.
    private val refreshClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val refreshRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val refreshAuthApi: AuthApi by lazy { refreshRetrofit.create(AuthApi::class.java) }

    private val tokenAuthenticator by lazy {
        TokenAuthenticator(
            tokenStore = tokenStore,
            refreshApi = refreshAuthApi,
            mutex = Mutex()
        )
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun init(tokenManager: TokenManager) {
        ApiClient.tokenManager = tokenManager
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val jobsApi: JobsApi by lazy { retrofit.create(JobsApi::class.java) }
    val adminApi: AdminApi by lazy { retrofit.create(AdminApi::class.java) }
    val assignmentsApi: AssignmentsApi by lazy { retrofit.create(AssignmentsApi::class.java) }
    val wagesApi: WagesApi by lazy { retrofit.create(WagesApi::class.java) }
}
