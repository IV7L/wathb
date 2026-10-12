package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.ApplicationActionRequest
import com.yawmiyati.app.data.api.JobApplicationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WorkerAppsUiState {
    object Loading : WorkerAppsUiState()
    object Empty : WorkerAppsUiState()
    data class Success(val applications: List<JobApplicationResponse>) : WorkerAppsUiState()
    data class Error(val message: String) : WorkerAppsUiState()
}

sealed class WorkerAppsActionState {
    object Idle : WorkerAppsActionState()
    object Loading : WorkerAppsActionState()
    data class Error(val message: String) : WorkerAppsActionState()
}

class WorkerApplicationsViewModel : ViewModel() {

    private val workersApi = ApiClient.workersApi
    private val jobsApi = ApiClient.jobsApi

    private val _uiState = MutableStateFlow<WorkerAppsUiState>(WorkerAppsUiState.Loading)
    val uiState: StateFlow<WorkerAppsUiState> = _uiState

    private val _actionState = MutableStateFlow<WorkerAppsActionState>(WorkerAppsActionState.Idle)
    val actionState: StateFlow<WorkerAppsActionState> = _actionState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = WorkerAppsUiState.Loading
            try {
                val response = workersApi.listApplications()
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    _uiState.value = if (results.isEmpty()) WorkerAppsUiState.Empty
                                     else WorkerAppsUiState.Success(results)
                } else {
                    _uiState.value = WorkerAppsUiState.Error("فشل تحميل الطلبات")
                }
            } catch (e: Exception) {
                _uiState.value = WorkerAppsUiState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun withdraw(jobId: String, appId: String) {
        if (_actionState.value is WorkerAppsActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WorkerAppsActionState.Loading
            try {
                val response = jobsApi.applicationAction(jobId, appId, ApplicationActionRequest("withdraw"))
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!
                    val current = _uiState.value
                    if (current is WorkerAppsUiState.Success) {
                        _uiState.value = WorkerAppsUiState.Success(
                            current.applications.map { if (it.id == updated.id) updated else it }
                        )
                    }
                    _actionState.value = WorkerAppsActionState.Idle
                } else {
                    _actionState.value = WorkerAppsActionState.Error("فشل سحب الطلب")
                }
            } catch (e: Exception) {
                _actionState.value = WorkerAppsActionState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun resetActionState() { _actionState.value = WorkerAppsActionState.Idle }
}
