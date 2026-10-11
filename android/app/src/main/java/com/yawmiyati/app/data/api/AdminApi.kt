package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("admin/businesses/")
    suspend fun listBusinesses(
        @Query("verification_status") status: String? = null
    ): Response<PaginatedResponse<AdminBusinessProfile>>

    @PATCH("admin/businesses/{pk}/verify/")
    suspend fun verifyBusiness(
        @Path("pk") pk: Int,
        @Body request: AdminVerifyRequest
    ): Response<AdminBusinessProfile>
}
