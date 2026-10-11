package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.JobApplicationResponse
import com.yawmiyati.app.data.jobs.JobRepository
import com.yawmiyati.app.data.jobs.JobResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ApplicationsUiState {
    object Loading : ApplicationsUiState()
    data class Success(val applications: List<JobApplicationResponse>) : ApplicationsUiState()
    data class Error(val message: String) : ApplicationsUiState()
    object Empty : ApplicationsUiState()
}

sealed class ApplicationActionState {
    object Idle : ApplicationActionState()
    object Loading : ApplicationActionState()
    data class Error(val message: String) : ApplicationActionState()
}

class JobApplicationsViewModel(
    private val repository: JobRepository = JobRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApplicationsUiState>(ApplicationsUiState.Loading)
    val uiState: StateFlow<ApplicationsUiState> = _uiState

    private val _actionState = MutableStateFlow<ApplicationActionState>(ApplicationActionState.Idle)
    val actionState: StateFlow<ApplicationActionState> = _actionState

    private var currentJobId: String = ""

    fun loadApplications(jobId: String) {
        currentJobId = jobId
        viewModelScope.launch {
            _uiState.value = ApplicationsUiState.Loading
            _uiState.value = when (val result = repository.listApplications(jobId)) {
                is JobResult.Success -> if (result.data.isEmpty()) ApplicationsUiState.Empty
                                        else ApplicationsUiState.Success(result.data)
                is JobResult.Error   -> ApplicationsUiState.Error(result.message)
            }
        }
    }

    fun offerApplication(appId: String) = performAction(appId, "offer")
    fun rejectApplication(appId: String) = performAction(appId, "reject")

    private fun performAction(appId: String, action: String) {
        if (_actionState.value is ApplicationActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = ApplicationActionState.Loading
            when (val result = repository.applicationAction(currentJobId, appId, action)) {
                is JobResult.Success -> {
                    _actionState.value = ApplicationActionState.Idle
                    // Replace the updated application in the list without a full reload.
                    val current = _uiState.value
                    if (current is ApplicationsUiState.Success) {
                        val updated = current.applications.map {
                            if (it.id == result.data.id) result.data else it
                        }
                        _uiState.value = ApplicationsUiState.Success(updated)
                    }
                }
                is JobResult.Error -> {
                    _actionState.value = ApplicationActionState.Error(result.message)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ApplicationActionState.Idle
    }
}
