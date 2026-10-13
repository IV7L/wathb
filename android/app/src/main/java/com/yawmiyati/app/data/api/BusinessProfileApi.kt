package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface BusinessProfileApi {
    @GET("businesses/profile/")
    suspend fun getProfile(): Response<BusinessProfileResponse>

    @PATCH("businesses/profile/")
    suspend fun updateProfile(@Body request: BusinessProfileUpdateRequest): Response<BusinessProfileResponse>
}
