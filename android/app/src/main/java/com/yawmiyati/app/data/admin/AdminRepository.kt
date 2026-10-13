package com.yawmiyati.app.data.admin

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yawmiyati.app.data.api.AdminBusinessProfile
import com.yawmiyati.app.data.api.AdminVerifyRequest
import com.yawmiyati.app.data.api.AdminWorkerActionRequest
import com.yawmiyati.app.data.api.AdminWorkerProfile
import com.yawmiyati.app.data.api.ApiClient

sealed class AdminResult<out T> {
    data class Success<T>(val data: T) : AdminResult<T>()
    data class Error(val message: String, val isAuth: Boolean = false) : AdminResult<Nothing>()
}

class AdminRepository {
    private val api = ApiClient.adminApi

    suspend fun listBusinesses(statusFilter: String? = null): AdminResult<List<AdminBusinessProfile>> {
        return try {
            val response = api.listBusinesses(status = statusFilter)
            if (response.isSuccessful) {
                AdminResult.Success(response.body()?.results ?: emptyList())
            } else {
                val isAuth = response.code() == 401 || response.code() == 403
                AdminResult.Error(parseError(response.errorBody()?.string()), isAuth)
            }
        } catch (e: Exception) {
            AdminResult.Error(networkMessage(e))
        }
    }

    suspend fun verifyBusiness(pk: Int, action: String, reason: String = ""): AdminResult<AdminBusinessProfile> {
        return try {
            val response = api.verifyBusiness(pk, AdminVerifyRequest(action, reason))
            if (response.isSuccessful) {
                AdminResult.Success(response.body()!!)
            } else {
                val code = response.code()
                val msg = when (code) {
                    400 -> parseError(response.errorBody()?.string())
                    403 -> "ليس لديك صلاحية إدارية"
                    404 -> "لم يتم العثور على الحساب"
                    else -> parseError(response.errorBody()?.string())
                }
                AdminResult.Error(msg, code == 401)
            }
        } catch (e: Exception) {
            AdminResult.Error(networkMessage(e))
        }
    }

    suspend fun listWorkers(isVerified: Boolean? = null, search: String? = null): AdminResult<List<AdminWorkerProfile>> {
        return try {
            val response = api.listWorkers(isVerified = isVerified, search = search?.takeIf { it.isNotBlank() })
            if (response.isSuccessful) {
                AdminResult.Success(response.body()?.results ?: emptyList())
            } else {
                AdminResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
            }
        } catch (e: Exception) {
            AdminResult.Error(networkMessage(e))
        }
    }

    suspend fun verifyWorker(pk: Int, action: String): AdminResult<AdminWorkerProfile> {
        return try {
            val response = api.verifyWorker(pk, AdminWorkerActionRequest(action))
            if (response.isSuccessful) {
                AdminResult.Success(response.body()!!)
            } else {
                val code = response.code()
                val msg = when (code) {
                    400 -> parseError(response.errorBody()?.string())
                    403 -> "ليس لديك صلاحية إدارية"
                    404 -> "لم يتم العثور على الحساب"
                    else -> parseError(response.errorBody()?.string())
                }
                AdminResult.Error(msg, code == 401)
            }
        } catch (e: Exception) {
            AdminResult.Error(networkMessage(e))
        }
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
