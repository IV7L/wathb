package com.yawmiyati.app.data.api

import com.yawmiyati.app.data.auth.SessionEventBus
import com.yawmiyati.app.data.auth.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApi: AuthApi,
    private val mutex: Mutex = Mutex()
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val staleToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?: return null

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenStore.getAccessToken()
                // Another coroutine already refreshed — retry with the new token.
                if (currentToken != null && currentToken != staleToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = tokenStore.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    signOut()
                    return@withLock null
                }

                val result = try {
                    refreshApi.refreshToken(RefreshRequest(refreshToken))
                } catch (_: Exception) {
                    null
                }

                if (result != null && result.isSuccessful) {
                    val body = result.body()!!
                    val newRefresh = body.refresh ?: refreshToken
                    tokenStore.saveTokens(body.access, newRefresh)
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
        tokenStore.clear()
        SessionEventBus.emitSessionExpired()
    }
}
