package com.yawmiyati.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.NotificationResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.NotificationsUiState
import com.yawmiyati.app.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasUnread = (uiState as? NotificationsUiState.Success)?.items?.any { !it.isRead } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإشعارات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (hasUnread) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "قراءة الكل")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is NotificationsUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is NotificationsUiState.Empty -> EmptyState(
                message = "لا توجد إشعارات",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
            is NotificationsUiState.Error -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
            is NotificationsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.items, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onRead = { if (!notification.isRead) viewModel.markRead(notification.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationResponse,
    onRead: () -> Unit
) {
    Card(
        onClick = onRead,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = notification.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (notification.body.isNotBlank()) {
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
