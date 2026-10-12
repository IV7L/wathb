package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.NotificationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    object Empty : NotificationsUiState()
    data class Success(val items: List<NotificationResponse>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState

    val unreadCount: StateFlow<Int> get() = _unreadCount
    private val _unreadCount = MutableStateFlow(0)

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                val resp = ApiClient.notificationsApi.list()
                if (resp.isSuccessful) {
                    val list = resp.body()?.results ?: emptyList()
                    _unreadCount.value = list.count { !it.isRead }
                    _uiState.value = if (list.isEmpty()) NotificationsUiState.Empty
                                     else NotificationsUiState.Success(list)
                } else {
                    _uiState.value = NotificationsUiState.Error("خطأ ${resp.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "خطأ في الاتصال")
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.notificationsApi.markRead(id)
                if (resp.isSuccessful) {
                    val current = (_uiState.value as? NotificationsUiState.Success)?.items ?: return@launch
                    val updated = current.map { if (it.id == id) it.copy(isRead = true) else it }
                    _unreadCount.value = updated.count { !it.isRead }
                    _uiState.value = NotificationsUiState.Success(updated)
                }
            } catch (_: Exception) {}
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.notificationsApi.markAllRead()
                if (resp.isSuccessful) {
                    val current = (_uiState.value as? NotificationsUiState.Success)?.items ?: return@launch
                    val updated = current.map { it.copy(isRead = true) }
                    _unreadCount.value = 0
                    _uiState.value = NotificationsUiState.Success(updated)
                }
            } catch (_: Exception) {}
        }
    }
}
