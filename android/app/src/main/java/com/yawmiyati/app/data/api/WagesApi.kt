package com.yawmiyati.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WagesApi {
    @GET("wages/")
    suspend fun listObligations(
        @Query("status") status: String? = null
    ): Response<PaginatedResponse<WageObligationResponse>>

    @GET("wages/{id}/")
    suspend fun getObligation(@Path("id") id: String): Response<WageObligationResponse>

    @POST("wages/{id}/declare-payment/")
    suspend fun declarePayment(
        @Path("id") id: String,
        @Body request: DeclarePaymentRequest
    ): Response<PaymentDeclarationResponse>

    @POST("wages/{id}/acknowledge/")
    suspend fun acknowledgePayment(@Path("id") id: String): Response<WageObligationResponse>

    @POST("wages/{id}/declarations/{decId}/verify/")
    suspend fun verifyDeclaration(
        @Path("id") id: String,
        @Path("decId") decId: String,
        @Body request: AdminVerifyDeclarationRequest
    ): Response<PaymentDeclarationResponse>
}
