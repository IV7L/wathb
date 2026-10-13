package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.BusinessProfileResponse
import com.yawmiyati.app.data.api.BusinessProfileUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BusinessProfileUiState {
    object Loading : BusinessProfileUiState()
    data class Success(val profile: BusinessProfileResponse) : BusinessProfileUiState()
    data class Error(val message: String) : BusinessProfileUiState()
}

sealed class BusinessProfileSaveState {
    object Idle : BusinessProfileSaveState()
    object Saving : BusinessProfileSaveState()
    object Saved : BusinessProfileSaveState()
    data class Error(val message: String) : BusinessProfileSaveState()
}

class BusinessProfileViewModel : ViewModel() {

    private val api = ApiClient.businessProfileApi

    private val _uiState = MutableStateFlow<BusinessProfileUiState>(BusinessProfileUiState.Loading)
    val uiState: StateFlow<BusinessProfileUiState> = _uiState

    private val _saveState = MutableStateFlow<BusinessProfileSaveState>(BusinessProfileSaveState.Idle)
    val saveState: StateFlow<BusinessProfileSaveState> = _saveState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = BusinessProfileUiState.Loading
            try {
                val response = api.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = BusinessProfileUiState.Success(response.body()!!)
                } else {
                    _uiState.value = BusinessProfileUiState.Error("فشل تحميل ملف المنشأة")
                }
            } catch (e: Exception) {
                _uiState.value = BusinessProfileUiState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun save(
        legalName: String,
        tradeName: String,
        locationAddress: String,
        locationCity: String,
        contactName: String,
        contactPhone: String,
    ) {
        if (_saveState.value is BusinessProfileSaveState.Saving) return
        viewModelScope.launch {
            _saveState.value = BusinessProfileSaveState.Saving
            try {
                val response = api.updateProfile(
                    BusinessProfileUpdateRequest(
                        legalName = legalName.trim(),
                        tradeName = tradeName.trim(),
                        locationAddress = locationAddress.trim(),
                        locationCity = locationCity.trim(),
                        contactName = contactName.trim(),
                        contactPhone = contactPhone.trim(),
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = BusinessProfileUiState.Success(response.body()!!)
                    _saveState.value = BusinessProfileSaveState.Saved
                } else {
                    _saveState.value = BusinessProfileSaveState.Error("فشل حفظ ملف المنشأة")
                }
            } catch (e: Exception) {
                _saveState.value = BusinessProfileSaveState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun resetSaveState() { _saveState.value = BusinessProfileSaveState.Idle }
}
