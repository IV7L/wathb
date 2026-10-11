package com.yawmiyati.app.data.api

import com.yawmiyati.app.BuildConfig
import com.yawmiyati.app.data.auth.SessionEventBus
import com.yawmiyati.app.data.auth.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var tokenManager: TokenManager? = null

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

    private val refreshMutex = Mutex()

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
            // Extract the token that was stale so we can detect concurrent-refresh situations.
            val staleToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?: return null // no token on the request — don't retry

            return runBlocking {
                refreshMutex.withLock {
                    val currentToken = tokenManager?.getAccessToken()
                    // Another coroutine already refreshed — retry with the new token.
                    if (currentToken != null && currentToken != staleToken) {
                        return@withLock response.request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    }

                    val refreshToken = tokenManager?.getRefreshToken()
                    if (refreshToken.isNullOrBlank()) {
                        signOut()
                        return@withLock null
                    }

                    val result = try {
                        refreshAuthApi.refreshToken(RefreshRequest(refreshToken))
                    } catch (_: Exception) {
                        null
                    }

                    if (result != null && result.isSuccessful) {
                        val body = result.body()!!
                        // ROTATE_REFRESH_TOKENS=True → backend returns a new refresh token.
                        val newRefresh = body.refresh ?: refreshToken
                        tokenManager?.saveTokens(body.access, newRefresh)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.access}")
                            .build()
                    } else {
                        signOut()
                        null
                    }
                }
            }
        }

        private fun signOut() {
            tokenManager?.clear()
            SessionEventBus.emitSessionExpired()
        }
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
