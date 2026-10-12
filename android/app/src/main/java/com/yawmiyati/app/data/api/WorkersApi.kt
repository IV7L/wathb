package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface WorkersApi {
    @GET("workers/profile/")
    suspend fun getProfile(): Response<WorkerProfileResponse>

    @PATCH("workers/profile/")
    suspend fun updateProfile(@Body request: WorkerProfileUpdateRequest): Response<WorkerProfileResponse>

    @GET("workers/applications/")
    suspend fun listApplications(): Response<PaginatedResponse<JobApplicationResponse>>
}
