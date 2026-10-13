package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.admin.AdminRepository
import com.yawmiyati.app.data.admin.AdminResult
import com.yawmiyati.app.data.api.PilotSummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val summary: PilotSummaryResponse) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

class AdminDashboardViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            _uiState.value = when (val r = repository.getPilotSummary()) {
                is AdminResult.Success -> DashboardUiState.Success(r.data)
                is AdminResult.Error   -> DashboardUiState.Error(r.message)
            }
        }
    }
}
