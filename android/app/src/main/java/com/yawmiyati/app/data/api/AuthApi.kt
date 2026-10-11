package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register/")
    suspend fun registerWorker(@Body request: RegisterWorkerRequest): Response<RegisterResponse>

    @POST("auth/register/")
    suspend fun registerBusiness(@Body request: RegisterBusinessRequest): Response<RegisterResponse>

    @POST("auth/token/refresh/")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<RefreshResponse>

    @POST("auth/logout/")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    @GET("auth/me/")
    suspend fun me(): Response<UserResponse>
}
