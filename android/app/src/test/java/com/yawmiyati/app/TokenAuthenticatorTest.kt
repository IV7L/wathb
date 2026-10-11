package com.yawmiyati.app

import com.yawmiyati.app.data.api.AuthApi
import com.yawmiyati.app.data.api.TokenAuthenticator
import com.yawmiyati.app.data.auth.SessionEventBus
import com.yawmiyati.app.data.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {

    private val server = MockWebServer()
    private lateinit var fakeTokenStore: FakeTokenStore
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()

        fakeTokenStore = FakeTokenStore(access = "stale_token", refresh = "refresh_token")

        // Separate refresh client — no authenticator to avoid recursion.
        val refreshClient = OkHttpClient.Builder().build()
        val refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        val authenticator = TokenAuthenticator(
            tokenStore = fakeTokenStore,
            refreshApi = refreshApi,
            mutex = Mutex()
        )

        client = OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor { chain ->
                val token = fakeTokenStore.getAccessToken()
                val req = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(req)
            }
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** (أ) 401 → تحديث الرمز → إعادة المحاولة بنجاح */
    @Test
    fun `401 triggers refresh then retries and succeeds`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody(""))
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"access":"new_access","refresh":"new_refresh"}""")
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val response = client.newCall(
            Request.Builder().url(server.url("/api/")).build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals("new_access", fakeTokenStore.getAccessToken())
        response.close()

        // Verify request sequence: GET /api/ → POST /auth/token/refresh/ → GET /api/
        val req1 = server.takeRequest(2, TimeUnit.SECONDS)
        val req2 = server.takeRequest(2, TimeUnit.SECONDS)
        val req3 = server.takeRequest(2, TimeUnit.SECONDS)
        assertTrue(req1?.path?.startsWith("/api/") == true)
        assertTrue(req2?.path?.contains("token/refresh") == true)
        assertTrue(req3?.path?.startsWith("/api/") == true)
    }

    /** (ب) طلبان متوازيان يُنتجان طلب تحديث واحد فقط */
    @Test
    fun `two parallel 401s cause exactly one refresh call`() = runBlocking {
        // Both parallel GETs get 401, one refresh succeeds, both retries succeed.
        server.enqueue(MockResponse().setResponseCode(401).setBody(""))
        server.enqueue(MockResponse().setResponseCode(401).setBody(""))
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"access":"new_access","refresh":"new_refresh"}""")
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok1"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok2"))

        val req = Request.Builder().url(server.url("/api/")).build()
        val d1 = async(Dispatchers.IO) { client.newCall(req).execute() }
        val d2 = async(Dispatchers.IO) { client.newCall(req).execute() }
        val r1 = d1.await()
        val r2 = d2.await()

        assertEquals(200, r1.code)
        assertEquals(200, r2.code)
        r1.close()
        r2.close()

        // Count refresh requests among all MockWebServer hits.
        val allRequests = (1..server.requestCount).map {
            server.takeRequest(1, TimeUnit.SECONDS)
        }
        val refreshCount = allRequests.count { it?.path?.contains("token/refresh") == true }
        assertEquals(1, refreshCount)
    }

    /** (ج) تحديث الرمز يُرجع 401 → مسح الرموز وإصدار sessionExpired مرة واحدة */
    @Test
    fun `refresh 401 clears tokens and emits sessionExpired exactly once`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody(""))
        server.enqueue(MockResponse().setResponseCode(401).setBody(""))

        val sessionExpiredCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val collectJob = launch(Dispatchers.Unconfined) {
            SessionEventBus.sessionExpired.collect {
                if (sessionExpiredCount.incrementAndGet() == 1) latch.countDown()
            }
        }

        client.newCall(
            Request.Builder().url(server.url("/api/")).build()
        ).execute().close()

        // Event is emitted synchronously inside the authenticator before execute() returns;
        // latch guards against any scheduler delay.
        assertTrue("sessionExpired not emitted within 2s", latch.await(2, TimeUnit.SECONDS))
        collectJob.cancel()

        assertTrue(fakeTokenStore.clearCalled)
        assertEquals(1, sessionExpiredCount.get())
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private class FakeTokenStore(access: String?, refresh: String?) : TokenStore {
        private var _access = access
        private var _refresh = refresh
        var clearCalled = false

        override fun getAccessToken(): String? = _access
        override fun getRefreshToken(): String? = _refresh
        override fun saveTokens(access: String, refresh: String) {
            _access = access
            _refresh = refresh
        }
        override fun clear() {
            clearCalled = true
            _access = null
            _refresh = null
        }
    }
}
