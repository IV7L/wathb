package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.YawmiyatiApp
import com.yawmiyati.app.data.api.UserResponse
import com.yawmiyati.app.data.auth.AuthRepository
import com.yawmiyati.app.data.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserResponse) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object LoggedOut : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val tokenManager = YawmiyatiApp.instance.tokenManager
    private val repository = AuthRepository(tokenManager)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(phone: String, password: String) {
        if (phone.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال رقم الهاتف وكلمة المرور")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = repository.login(phone, password)) {
                is AuthResult.Success -> AuthUiState.Success(result.data)
                is AuthResult.Error -> AuthUiState.Error(result.message)
            }
        }
    }

    fun registerWorker(phone: String, password: String, fullName: String, consentVersion: Int) {
        if (phone.isBlank() || password.isBlank() || fullName.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى ملء جميع الحقول المطلوبة")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = repository.registerWorker(
                phone, password, fullName, consentVersion)) {
                is AuthResult.Success -> AuthUiState.Success(result.data)
                is AuthResult.Error -> AuthUiState.Error(result.message)
            }
        }
    }

    fun registerBusiness(
        phone: String, password: String,
        legalName: String, contactName: String, contactPhone: String, consentVersion: Int
    ) {
        if (phone.isBlank() || password.isBlank() || legalName.isBlank() ||
            contactName.isBlank() || contactPhone.isBlank()
        ) {
            _uiState.value = AuthUiState.Error("يرجى ملء جميع الحقول المطلوبة")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = repository.registerBusiness(
                phone, password, legalName, contactName, contactPhone, consentVersion
            )) {
                is AuthResult.Success -> AuthUiState.Success(result.data)
                is AuthResult.Error -> AuthUiState.Error(result.message)
            }
        }
    }

    fun loginWithBiometric() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = repository.refreshSession()) {
                is AuthResult.Success -> AuthUiState.Success(result.data)
                is AuthResult.Error   -> AuthUiState.Error(result.message)
            }
        }
    }

    /** Lock app: access token removed locally, refresh token + biometric flag kept. No server call. */
    fun lockApp() {
        repository.lockApp()
        _uiState.value = AuthUiState.LoggedOut
    }

    /** Full logout: blacklists refresh token on server, wipes all local tokens + biometric flag. */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.LoggedOut
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
