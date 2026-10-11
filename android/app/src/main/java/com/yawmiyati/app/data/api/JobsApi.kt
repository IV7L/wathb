package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JobsApi {
    @GET("jobs/")
    suspend fun listJobs(
        @Query("page") page: Int = 1,
        @Query("city") city: String? = null,
        @Query("work_date") workDate: String? = null
    ): Response<PaginatedResponse<JobListingResponse>>

    @GET("jobs/{id}/")
    suspend fun getJob(@Path("id") id: String): Response<JobListingResponse>

    @POST("jobs/")
    suspend fun createJob(@Body request: JobCreateRequest): Response<JobListingResponse>

    @POST("jobs/{id}/submit/")
    suspend fun submitJob(@Path("id") id: String): Response<JobListingResponse>

    @POST("jobs/{id}/publish/")
    suspend fun publishJob(@Path("id") id: String): Response<JobListingResponse>

    @POST("jobs/{id}/apply/")
    suspend fun applyToJob(
        @Path("id") id: String,
        @Body request: ApplyRequest
    ): Response<JobApplicationResponse>

    @GET("jobs/{id}/applications/")
    suspend fun listApplications(
        @Path("id") id: String
    ): Response<PaginatedResponse<JobApplicationResponse>>

    @PATCH("jobs/{id}/applications/{appId}/")
    suspend fun applicationAction(
        @Path("id") id: String,
        @Path("appId") appId: String,
        @Body request: ApplicationActionRequest
    ): Response<JobApplicationResponse>
}
