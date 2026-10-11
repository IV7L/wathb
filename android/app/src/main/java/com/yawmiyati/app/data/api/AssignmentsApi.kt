package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AssignmentsApi {
    @GET("assignments/")
    suspend fun listAssignments(): Response<PaginatedResponse<AssignmentResponse>>

    @POST("assignments/{id}/accept/")
    suspend fun acceptAssignment(@Path("id") id: String): Response<AssignmentResponse>

    @POST("assignments/{id}/decline/")
    suspend fun declineAssignment(@Path("id") id: String): Response<AssignmentResponse>

    @GET("assignments/{id}/attendance/")
    suspend fun listAttendance(@Path("id") id: String): Response<List<AttendanceResponse>>

    @POST("assignments/{id}/attendance/")
    suspend fun submitAttendance(
        @Path("id") id: String,
        @Body request: AttendanceSubmitRequest
    ): Response<AttendanceResponse>

    @PATCH("assignments/{id}/attendance/{recId}/confirm/")
    suspend fun confirmAttendance(
        @Path("id") id: String,
        @Path("recId") recId: String
    ): Response<AttendanceResponse>

    @GET("assignments/{id}/completion/")
    suspend fun getCompletion(@Path("id") id: String): Response<CompletionResponse>

    @POST("assignments/{id}/completion/")
    suspend fun submitCompletion(
        @Path("id") id: String,
        @Body request: CompletionSubmitRequest
    ): Response<CompletionResponse>

    @PATCH("assignments/{id}/completion/confirm/")
    suspend fun confirmCompletion(@Path("id") id: String): Response<CompletionResponse>
}
