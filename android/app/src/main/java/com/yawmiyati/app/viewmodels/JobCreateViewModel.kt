package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.JobCreateRequest
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.data.jobs.JobRepository
import com.yawmiyati.app.data.jobs.JobResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class JobCreateUiState {
    object Idle : JobCreateUiState()
    object Loading : JobCreateUiState()
    data class Success(val job: JobListingResponse) : JobCreateUiState()
    data class Error(val message: String) : JobCreateUiState()
}

class JobCreateViewModel(
    private val repository: JobRepository = JobRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<JobCreateUiState>(JobCreateUiState.Idle)
    val uiState: StateFlow<JobCreateUiState> = _uiState

    fun createJob(
        title: String,
        description: String,
        locationAddress: String,
        locationCity: String,
        workDate: String,       // YYYY-MM-DD
        startTime: String,      // HH:MM
        endTime: String,        // HH:MM
        numWorkers: Int,
        wageAmountIls: Long,    // in full ILS — we convert to agorot (×100) here
        wageBasis: String,
        paymentMethod: String,
        paymentDeadlineDays: Int
    ) {
        viewModelScope.launch {
            _uiState.value = JobCreateUiState.Loading
            val request = JobCreateRequest(
                title = title.trim(),
                description = description.trim(),
                locationAddress = locationAddress.trim(),
                locationCity = locationCity.trim(),
                workDate = workDate.trim(),
                startTime = startTime.trim(),
                endTime = endTime.trim(),
                numWorkersNeeded = numWorkers,
                wageAmount = wageAmountIls * 100,   // store in minor units
                wageBasis = wageBasis,
                paymentMethod = paymentMethod,
                paymentDeadlineDays = paymentDeadlineDays
            )
            _uiState.value = when (val result = repository.createJob(request)) {
                is JobResult.Success -> JobCreateUiState.Success(result.data)
                is JobResult.Error   -> JobCreateUiState.Error(result.message)
            }
        }
    }

    fun reset() { _uiState.value = JobCreateUiState.Idle }
}
