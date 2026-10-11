package com.yawmiyati.app.data.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.LoginRequest
import com.yawmiyati.app.data.api.RefreshRequest
import com.yawmiyati.app.data.api.RegisterBusinessRequest
import com.yawmiyati.app.data.api.RegisterWorkerRequest
import com.yawmiyati.app.data.api.UserResponse

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

class AuthRepository(private val tokenManager: TokenManager) {
    private val api = ApiClient.authApi

    suspend fun login(email: String, password: String): AuthResult<UserResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveTokens(body.access, body.refresh)
                val meResponse = api.me()
                if (meResponse.isSuccessful) {
                    val user = meResponse.body()!!
                    tokenManager.saveRole(user.role)
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("تعذّر تحميل ملف المستخدم")
                }
            } else {
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error(networkMessage(e))
        }
    }

    suspend fun registerWorker(
        email: String, password: String, fullName: String, phone: String?
    ): AuthResult<UserResponse> {
        return try {
            val response = api.registerWorker(
                RegisterWorkerRequest(email, password, fullName = fullName, phone = phone)
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                // Registration returns tokens — save them so user is logged in immediately
                tokenManager.saveTokens(body.access, body.refresh)
                tokenManager.saveRole(body.user.role)
                AuthResult.Success(body.user)
            } else {
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error(networkMessage(e))
        }
    }

    suspend fun registerBusiness(
        email: String, password: String,
        legalName: String, contactName: String, contactPhone: String, phone: String?
    ): AuthResult<UserResponse> {
        return try {
            val response = api.registerBusiness(
                RegisterBusinessRequest(
                    email, password,
                    legalName = legalName,
                    contactName = contactName,
                    contactPhone = contactPhone,
                    phone = phone
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveTokens(body.access, body.refresh)
                tokenManager.saveRole(body.user.role)
                AuthResult.Success(body.user)
            } else {
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error(networkMessage(e))
        }
    }

    suspend fun logout() {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken != null) {
            try { api.logout(RefreshRequest(refreshToken)) } catch (_: Exception) {}
        }
        tokenManager.clear()
    }

    private fun parseErrorMessage(body: String?): String {
        if (body.isNullOrBlank()) return "خطأ غير معروف"
        return try {
            val json = Gson().fromJson(body, JsonObject::class.java)
            when {
                json.has("detail") -> json["detail"].asString
                json.has("non_field_errors") ->
                    json["non_field_errors"].asJsonArray.firstOrNull()?.asString ?: "بيانات غير صالحة"
                else -> {
                    // Return first field-level error
                    json.entrySet().firstOrNull()?.let { (key, value) ->
                        val msg = if (value.isJsonArray) value.asJsonArray.firstOrNull()?.asString
                        else value.asString
                        "$key: $msg"
                    } ?: "خطأ في الطلب"
                }
            }
        } catch (_: Exception) {
            "فشل الطلب"
        }
    }

    private fun networkMessage(e: Exception): String =
        if (e.message?.contains("Unable to resolve host") == true ||
            e.message?.contains("Failed to connect") == true)
            "تعذّر الاتصال بالخادم"
        else e.localizedMessage ?: "خطأ في الشبكة"
}
