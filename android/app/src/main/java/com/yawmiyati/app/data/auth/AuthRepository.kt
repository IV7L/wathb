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

    suspend fun login(phone: String, password: String): AuthResult<UserResponse> {
        return try {
            val response = api.login(LoginRequest(phone, password))
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
        phone: String, password: String, fullName: String, consentVersion: Int
    ): AuthResult<UserResponse> {
        return try {
            val response = api.registerWorker(
                RegisterWorkerRequest(phone, password, fullName = fullName,
                    consentVersion = consentVersion)
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
        phone: String, password: String,
        legalName: String, contactName: String, contactPhone: String, consentVersion: Int
    ): AuthResult<UserResponse> {
        return try {
            val response = api.registerBusiness(
                RegisterBusinessRequest(
                    phone, password,
                    legalName = legalName,
                    contactName = contactName,
                    contactPhone = contactPhone,
                    consentVersion = consentVersion
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
        // Soft logout: keep refresh token so biometric re-login works next time.
        tokenManager.softLogout()
    }

    /** Exchange the stored refresh token for a new access token (used by biometric login). */
    suspend fun refreshSession(): AuthResult<UserResponse> {
        val refresh = tokenManager.getRefreshToken()
            ?: return AuthResult.Error("لا توجد جلسة محفوظة")
        return try {
            val response = api.refreshToken(RefreshRequest(refresh))
            if (response.isSuccessful) {
                val body = response.body()!!
                val newRefresh = body.refresh ?: refresh
                tokenManager.saveTokens(body.access, newRefresh)
                val meResponse = api.me()
                if (meResponse.isSuccessful) {
                    val user = meResponse.body()!!
                    tokenManager.saveRole(user.role)
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("تعذّر تحميل ملف المستخدم")
                }
            } else {
                // Refresh token expired → clear everything, force manual login
                tokenManager.clear()
                AuthResult.Error("انتهت صلاحية الجلسة، سجّل دخولك مجدداً")
            }
        } catch (e: Exception) {
            AuthResult.Error(networkMessage(e))
        }
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

    private fun networkMessage(e: Exception): String = when {
        e is java.net.SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم"
        e.message?.contains("Unable to resolve host") == true ||
            e.message?.contains("Failed to connect") == true -> "تعذّر الاتصال بالخادم"
        else -> e.localizedMessage ?: "خطأ في الشبكة"
    }
}
