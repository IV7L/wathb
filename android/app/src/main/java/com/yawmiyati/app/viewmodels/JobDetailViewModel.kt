package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.data.jobs.JobRepository
import com.yawmiyati.app.data.jobs.JobResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class JobDetailUiState {
    object Loading : JobDetailUiState()
    data class Success(val job: JobListingResponse) : JobDetailUiState()
    data class Error(val message: String) : JobDetailUiState()
}

sealed class ApplyUiState {
    object Idle : ApplyUiState()
    object Loading : ApplyUiState()
    // Applied and AlreadyApplied are terminal — they never reset to Idle,
    // so the apply button stays permanently disabled after one outcome.
    object Applied : ApplyUiState()
    object AlreadyApplied : ApplyUiState()
    data class Error(val message: String) : ApplyUiState()
}

class JobDetailViewModel(
    private val repository: JobRepository = JobRepository()
) : ViewModel() {

    private val _jobState = MutableStateFlow<JobDetailUiState>(JobDetailUiState.Loading)
    val jobState: StateFlow<JobDetailUiState> = _jobState

    private val _applyState = MutableStateFlow<ApplyUiState>(ApplyUiState.Idle)
    val applyState: StateFlow<ApplyUiState> = _applyState

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            _jobState.value = JobDetailUiState.Loading
            _jobState.value = when (val result = repository.getJob(jobId)) {
                is JobResult.Success -> JobDetailUiState.Success(result.data)
                is JobResult.Error   -> JobDetailUiState.Error(result.message)
            }
        }
    }

    fun applyToJob(jobId: String, coverNote: String = "") {
        val current = _applyState.value
        if (current is ApplyUiState.Loading ||
            current is ApplyUiState.Applied ||
            current is ApplyUiState.AlreadyApplied) return
        viewModelScope.launch {
            _applyState.value = ApplyUiState.Loading
            _applyState.value = when (val result = repository.applyToJob(jobId, coverNote)) {
                is JobResult.Success -> ApplyUiState.Applied
                is JobResult.Error   -> when {
                    result.isAlreadyApplied -> ApplyUiState.AlreadyApplied
                    else                    -> ApplyUiState.Error(result.message)
                }
            }
        }
    }

    fun resetApplyState() {
        // Only reset from Error; Applied and AlreadyApplied are permanent for this session.
        if (_applyState.value is ApplyUiState.Error) {
            _applyState.value = ApplyUiState.Idle
        }
    }
}
