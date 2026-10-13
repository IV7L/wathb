package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.admin.AdminRepository
import com.yawmiyati.app.data.admin.AdminResult
import com.yawmiyati.app.data.api.AdminWorkerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AdminWorkersUiState {
    object Loading : AdminWorkersUiState()
    data class Success(val workers: List<AdminWorkerProfile>) : AdminWorkersUiState()
    data class Error(val message: String) : AdminWorkersUiState()
    object Empty : AdminWorkersUiState()
}

sealed class WorkerActionState {
    object Idle : WorkerActionState()
    object Loading : WorkerActionState()
    data class Error(val message: String) : WorkerActionState()
}

class AdminWorkersViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminWorkersUiState>(AdminWorkersUiState.Loading)
    val uiState: StateFlow<AdminWorkersUiState> = _uiState

    private val _actionState = MutableStateFlow<WorkerActionState>(WorkerActionState.Idle)
    val actionState: StateFlow<WorkerActionState> = _actionState

    private var currentSearch: String? = null
    private var currentVerifiedFilter: Boolean? = null

    init { loadWorkers() }

    fun loadWorkers(isVerified: Boolean? = currentVerifiedFilter, search: String? = currentSearch) {
        currentVerifiedFilter = isVerified
        currentSearch = search
        viewModelScope.launch {
            _uiState.value = AdminWorkersUiState.Loading
            _uiState.value = when (val result = repository.listWorkers(isVerified, search)) {
                is AdminResult.Success -> if (result.data.isEmpty()) AdminWorkersUiState.Empty
                                         else AdminWorkersUiState.Success(result.data)
                is AdminResult.Error   -> AdminWorkersUiState.Error(result.message)
            }
        }
    }

    fun verifyWorker(pk: Int) {
        if (_actionState.value is WorkerActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WorkerActionState.Loading
            when (val result = repository.verifyWorker(pk, "verify")) {
                is AdminResult.Success -> {
                    _actionState.value = WorkerActionState.Idle
                    loadWorkers()
                }
                is AdminResult.Error -> _actionState.value = WorkerActionState.Error(result.message)
            }
        }
    }

    fun suspendWorker(pk: Int) {
        if (_actionState.value is WorkerActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WorkerActionState.Loading
            when (val result = repository.verifyWorker(pk, "suspend")) {
                is AdminResult.Success -> {
                    _actionState.value = WorkerActionState.Idle
                    loadWorkers()
                }
                is AdminResult.Error -> _actionState.value = WorkerActionState.Error(result.message)
            }
        }
    }

    fun resetActionState() { _actionState.value = WorkerActionState.Idle }
}
