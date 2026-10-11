package com.yawmiyati.app.data.jobs

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.ApplicationActionRequest
import com.yawmiyati.app.data.api.ApplyRequest
import com.yawmiyati.app.data.api.JobApplicationResponse
import com.yawmiyati.app.data.api.JobCreateRequest
import com.yawmiyati.app.data.api.JobListingResponse

sealed class JobResult<out T> {
    data class Success<T>(val data: T) : JobResult<T>()
    data class Error(
        val message: String,
        val isAuth: Boolean = false,
        val isAlreadyApplied: Boolean = false
    ) : JobResult<Nothing>()
}

open class JobRepository {
    private val api = ApiClient.jobsApi

    open suspend fun listJobs(city: String? = null, page: Int = 1): JobResult<PaginatedJobResult> {
        return try {
            val response = api.listJobs(page = page, city = city)
            if (response.isSuccessful) {
                val body = response.body()!!
                JobResult.Success(PaginatedJobResult(body.results, body.next != null, body.count))
            } else {
                val isAuth = response.code() == 401 || response.code() == 403
                JobResult.Error(parseError(response.errorBody()?.string()), isAuth)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    data class PaginatedJobResult(
        val jobs: List<JobListingResponse>,
        val hasNextPage: Boolean,
        val totalCount: Int
    )

    suspend fun getJob(id: String): JobResult<JobListingResponse> {
        return try {
            val response = api.getJob(id)
            if (response.isSuccessful) {
                JobResult.Success(response.body()!!)
            } else {
                JobResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    suspend fun applyToJob(jobId: String, coverNote: String = ""): JobResult<JobApplicationResponse> {
        return try {
            val response = api.applyToJob(jobId, ApplyRequest(coverNote))
            if (response.isSuccessful) {
                JobResult.Success(response.body()!!)
            } else {
                val code = response.code()
                val msg = when (code) {
                    409 -> "لقد تقدّمت لهذه الوظيفة من قبل"
                    403 -> "غير مصرح لك بالتقدم لهذه الوظيفة"
                    else -> parseError(response.errorBody()?.string())
                }
                JobResult.Error(msg, isAuth = code == 401, isAlreadyApplied = code == 409)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    suspend fun createJob(request: JobCreateRequest): JobResult<JobListingResponse> {
        return try {
            val response = api.createJob(request)
            if (response.isSuccessful) {
                JobResult.Success(response.body()!!)
            } else {
                val code = response.code()
                val msg = when (code) {
                    403 -> "حسابك لم يتم التحقق منه بعد. يرجى انتظار موافقة الإدارة."
                    else -> parseError(response.errorBody()?.string())
                }
                JobResult.Error(msg, code == 401)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    open suspend fun listApplications(jobId: String): JobResult<List<JobApplicationResponse>> {
        return try {
            val response = api.listApplications(jobId)
            if (response.isSuccessful) {
                JobResult.Success(response.body()?.results ?: emptyList())
            } else {
                JobResult.Error(parseError(response.errorBody()?.string()), response.code() == 401)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    open suspend fun applicationAction(
        jobId: String,
        appId: String,
        action: String
    ): JobResult<JobApplicationResponse> {
        return try {
            val response = api.applicationAction(jobId, appId, ApplicationActionRequest(action))
            if (response.isSuccessful) {
                JobResult.Success(response.body()!!)
            } else {
                val code = response.code()
                val msg = when (code) {
                    400 -> parseError(response.errorBody()?.string())
                    403 -> "غير مصرح"
                    404 -> "الطلب غير موجود"
                    else -> parseError(response.errorBody()?.string())
                }
                JobResult.Error(msg, code == 401)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    suspend fun submitAndPublishJob(jobId: String): JobResult<JobListingResponse> {
        return try {
            val submitResponse = api.submitJob(jobId)
            if (!submitResponse.isSuccessful) {
                val code = submitResponse.code()
                return JobResult.Error(parseError(submitResponse.errorBody()?.string()), code == 401)
            }
            val publishResponse = api.publishJob(jobId)
            if (publishResponse.isSuccessful) {
                JobResult.Success(publishResponse.body()!!)
            } else {
                val code = publishResponse.code()
                JobResult.Error(parseError(publishResponse.errorBody()?.string()), code == 401)
            }
        } catch (e: Exception) {
            JobResult.Error(networkMessage(e))
        }
    }

    private fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "خطأ غير معروف"
        return try {
            val json = Gson().fromJson(body, JsonObject::class.java)
            when {
                json.has("detail") -> json["detail"].asString
                else -> json.entrySet().firstOrNull()?.let { (k, v) ->
                    val msg = if (v.isJsonArray) v.asJsonArray.firstOrNull()?.asString else v.asString
                    "$k: $msg"
                } ?: "خطأ في الطلب"
            }
        } catch (_: Exception) { "فشل الطلب" }
    }

    private fun networkMessage(e: Exception): String =
        if (e.message?.contains("Unable to resolve host") == true ||
            e.message?.contains("Failed to connect") == true)
            "تعذّر الاتصال بالخادم"
        else e.localizedMessage ?: "خطأ في الشبكة"
}
