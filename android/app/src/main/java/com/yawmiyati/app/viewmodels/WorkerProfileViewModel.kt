package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.WorkerProfileResponse
import com.yawmiyati.app.data.api.WorkerProfileUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WorkerProfileUiState {
    object Loading : WorkerProfileUiState()
    data class Success(val profile: WorkerProfileResponse) : WorkerProfileUiState()
    data class Error(val message: String) : WorkerProfileUiState()
}

sealed class WorkerProfileSaveState {
    object Idle : WorkerProfileSaveState()
    object Saving : WorkerProfileSaveState()
    object Saved : WorkerProfileSaveState()
    data class Error(val message: String) : WorkerProfileSaveState()
}

class WorkerProfileViewModel : ViewModel() {

    private val api = ApiClient.workersApi

    private val _uiState = MutableStateFlow<WorkerProfileUiState>(WorkerProfileUiState.Loading)
    val uiState: StateFlow<WorkerProfileUiState> = _uiState

    private val _saveState = MutableStateFlow<WorkerProfileSaveState>(WorkerProfileSaveState.Idle)
    val saveState: StateFlow<WorkerProfileSaveState> = _saveState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = WorkerProfileUiState.Loading
            try {
                val response = api.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = WorkerProfileUiState.Success(response.body()!!)
                } else {
                    _uiState.value = WorkerProfileUiState.Error("فشل تحميل الملف الشخصي")
                }
            } catch (e: Exception) {
                _uiState.value = WorkerProfileUiState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun save(fullName: String, locationCity: String, locationRegion: String, isAvailable: Boolean) {
        if (_saveState.value is WorkerProfileSaveState.Saving) return
        viewModelScope.launch {
            _saveState.value = WorkerProfileSaveState.Saving
            try {
                val response = api.updateProfile(
                    WorkerProfileUpdateRequest(
                        fullName = fullName.trim(),
                        locationCity = locationCity.trim(),
                        locationRegion = locationRegion.trim(),
                        isAvailable = isAvailable,
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = WorkerProfileUiState.Success(response.body()!!)
                    _saveState.value = WorkerProfileSaveState.Saved
                } else {
                    _saveState.value = WorkerProfileSaveState.Error("فشل حفظ الملف الشخصي")
                }
            } catch (e: Exception) {
                _saveState.value = WorkerProfileSaveState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun resetSaveState() { _saveState.value = WorkerProfileSaveState.Idle }
}
