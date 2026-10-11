package com.yawmiyati.app.data.wages

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yawmiyati.app.data.api.AdminVerifyDeclarationRequest
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.DeclarePaymentRequest
import com.yawmiyati.app.data.api.DisputePaymentRequest
import com.yawmiyati.app.data.api.PaymentDeclarationResponse
import com.yawmiyati.app.data.api.WageObligationResponse

sealed class WageResult<out T> {
    data class Success<T>(val data: T) : WageResult<T>()
    data class Error(val message: String, val isAuth: Boolean = false) : WageResult<Nothing>()
}

open class WageRepository {
    private val api = ApiClient.wagesApi

    open suspend fun listObligations(status: String? = null): WageResult<List<WageObligationResponse>> {
        return try {
            val response = api.listObligations(status)
            if (response.isSuccessful) WageResult.Success(response.body()?.results ?: emptyList())
            else WageResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
        } catch (e: Exception) { WageResult.Error(networkMessage(e)) }
    }

    open suspend fun getObligation(id: String): WageResult<WageObligationResponse> {
        return try {
            val response = api.getObligation(id)
            if (response.isSuccessful) WageResult.Success(response.body()!!)
            else WageResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
        } catch (e: Exception) { WageResult.Error(networkMessage(e)) }
    }

    open suspend fun declarePayment(id: String, request: DeclarePaymentRequest): WageResult<PaymentDeclarationResponse> {
        return try {
            val response = api.declarePayment(id, request)
            if (response.isSuccessful) WageResult.Success(response.body()!!)
            else WageResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
        } catch (e: Exception) { WageResult.Error(networkMessage(e)) }
    }

    open suspend fun acknowledgePayment(id: String): WageResult<WageObligationResponse> {
        return try {
            val response = api.acknowledgePayment(id)
            if (response.isSuccessful) WageResult.Success(response.body()!!)
            else WageResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
        } catch (e: Exception) { WageResult.Error(networkMessage(e)) }
    }

    open suspend fun disputePayment(id: String, description: String): WageResult<WageObligationResponse> {
        return try {
            val response = api.disputePayment(id, DisputePaymentRequest(description = description))
            if (response.isSuccessful) WageResult.Success(response.body()!!)
            else WageResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
        } catch (e: Exception) { WageResult.Error(networkMessage(e)) }
    }

    open suspend fun verifyDeclaration(
        obligationId: String,
        declarationId: String,
        request: AdminVerifyDeclarationRequest
    ): WageResult<PaymentDeclarationResponse> {
        return try {
            val response = api.verifyDeclaration(obligationId, declarationId, request)
            if (response.isSuccessful) WageResult.Success(response.body()!!)
            else WageResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
        } catch (e: Exception) { WageResult.Error(networkMessage(e)) }
    }

    private fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "خطأ غير معروف"
        return try {
            val json = Gson().fromJson(body, JsonObject::class.java)
            when {
                json.has("detail") -> json["detail"].asString
                else -> json.entrySet().firstOrNull()?.let { (_, v) ->
                    if (v.isJsonArray) v.asJsonArray.firstOrNull()?.asString else v.asString
                } ?: "خطأ في الطلب"
            }
        } catch (_: Exception) { "فشل الطلب" }
    }

    private fun networkMessage(e: Exception): String = when {
        e is java.net.SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم"
        e.message?.contains("Unable to resolve host") == true ||
            e.message?.contains("Failed to connect") == true -> "تعذّر الاتصال بالخادم"
        else -> e.localizedMessage ?: "خطأ في الشبكة"
    }
}
