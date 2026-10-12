package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationsApi {
    @GET("notifications/")
    suspend fun list(): Response<PaginatedResponse<NotificationResponse>>

    @PATCH("notifications/{pk}/read/")
    suspend fun markRead(@Path("pk") pk: String): Response<NotificationResponse>

    @POST("notifications/read-all/")
    suspend fun markAllRead(): Response<Unit>
}
