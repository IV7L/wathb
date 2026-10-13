package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.data.jobs.JobRepository
import com.yawmiyati.app.data.jobs.JobResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PublishUiState {
    object Idle : PublishUiState()
    data class Loading(val jobId: String) : PublishUiState()
    data class Error(val message: String) : PublishUiState()
}

sealed class CloneJobUiState {
    object Idle : CloneJobUiState()
    object Cloning : CloneJobUiState()
    object Cloned : CloneJobUiState()
    data class Error(val message: String) : CloneJobUiState()
}

sealed class CancelJobUiState {
    object Idle : CancelJobUiState()
    data class Loading(val jobId: String) : CancelJobUiState()
    object Cancelled : CancelJobUiState()
    data class Error(val message: String) : CancelJobUiState()
}

sealed class JobsUiState {
    object Loading : JobsUiState()
    data class Success(
        val jobs: List<JobListingResponse>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false
    ) : JobsUiState()
    data class Error(val message: String, val isAuth: Boolean = false) : JobsUiState()
    object Empty : JobsUiState()
}

class JobsViewModel(private val repository: JobRepository = JobRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<JobsUiState>(JobsUiState.Loading)
    val uiState: StateFlow<JobsUiState> = _uiState

    private var currentPage = 1
    private var currentCity: String? = null
    private val accumulatedJobs = mutableListOf<JobListingResponse>()

    init {
        loadJobs()
    }

    fun loadJobs(city: String? = null) {
        currentPage = 1
        currentCity = city
        accumulatedJobs.clear()
        viewModelScope.launch {
            _uiState.value = JobsUiState.Loading
            fetchPage()
        }
    }

    fun refresh() {
        currentPage = 1
        currentCity = currentCity
        viewModelScope.launch {
            val current = _uiState.value
            if (current is JobsUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            accumulatedJobs.clear()
            fetchPage(isRefresh = true)
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (current !is JobsUiState.Success || !current.hasMore || current.isLoadingMore) return
        currentPage++
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            fetchPage()
        }
    }

    private val _publishState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val publishState: StateFlow<PublishUiState> = _publishState

    fun publishJob(jobId: String) {
        if (_publishState.value is PublishUiState.Loading) return
        viewModelScope.launch {
            _publishState.value = PublishUiState.Loading(jobId)
            when (val result = repository.submitAndPublishJob(jobId)) {
                is JobResult.Success -> {
                    _publishState.value = PublishUiState.Idle
                    refresh()
                }
                is JobResult.Error -> {
                    _publishState.value = PublishUiState.Error(result.message)
                }
            }
        }
    }

    fun resetPublishError() {
        _publishState.value = PublishUiState.Idle
    }

    private val _cancelState = MutableStateFlow<CancelJobUiState>(CancelJobUiState.Idle)
    val cancelState: StateFlow<CancelJobUiState> = _cancelState

    fun cancelJob(jobId: String, reason: String) {
        if (_cancelState.value is CancelJobUiState.Loading) return
        viewModelScope.launch {
            _cancelState.value = CancelJobUiState.Loading(jobId)
            when (val result = repository.cancelJob(jobId, reason)) {
                is JobResult.Success -> {
                    _cancelState.value = CancelJobUiState.Cancelled
                    refresh()
                }
                is JobResult.Error -> _cancelState.value = CancelJobUiState.Error(result.message)
            }
        }
    }

    fun resetCancelState() { _cancelState.value = CancelJobUiState.Idle }

    private val _cloneState = MutableStateFlow<CloneJobUiState>(CloneJobUiState.Idle)
    val cloneState: StateFlow<CloneJobUiState> = _cloneState

    fun cloneJob(jobId: String) {
        if (_cloneState.value is CloneJobUiState.Cloning) return
        viewModelScope.launch {
            _cloneState.value = CloneJobUiState.Cloning
            when (val result = repository.cloneJob(jobId)) {
                is JobResult.Success -> {
                    _cloneState.value = CloneJobUiState.Cloned
                    refresh()
                }
                is JobResult.Error -> _cloneState.value = CloneJobUiState.Error(result.message)
            }
        }
    }

    fun resetCloneState() { _cloneState.value = CloneJobUiState.Idle }

    private suspend fun fetchPage(isRefresh: Boolean = false) {
        when (val result = repository.listJobs(city = currentCity, page = currentPage)) {
            is JobResult.Success -> {
                accumulatedJobs.addAll(result.data.jobs)
                _uiState.value = if (accumulatedJobs.isEmpty()) JobsUiState.Empty
                else JobsUiState.Success(
                    jobs = accumulatedJobs.toList(),
                    hasMore = result.data.hasNextPage,
                    isLoadingMore = false,
                    isRefreshing = false
                )
            }
            is JobResult.Error -> {
                if (isRefresh || currentPage == 1) {
                    _uiState.value = JobsUiState.Error(result.message, result.isAuth)
                } else {
                    // Failed to load next page — revert page counter and show current list
                    currentPage--
                    val current = _uiState.value
                    if (current is JobsUiState.Success) {
                        _uiState.value = current.copy(isLoadingMore = false)
                    }
                }
            }
        }
    }
}
