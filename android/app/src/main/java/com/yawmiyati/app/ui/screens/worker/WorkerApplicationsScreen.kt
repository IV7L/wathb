package com.yawmiyati.app.ui.screens.worker

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobApplicationResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.WorkerAppsActionState
import com.yawmiyati.app.viewmodels.WorkerAppsUiState
import com.yawmiyati.app.viewmodels.WorkerApplicationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerApplicationsScreen(
    onBack: () -> Unit,
    viewModel: WorkerApplicationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        if (actionState is WorkerAppsActionState.Error) {
            snackbarHostState.showSnackbar((actionState as WorkerAppsActionState.Error).message)
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طلباتي") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is WorkerAppsUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is WorkerAppsUiState.Empty   -> EmptyState(
                message = "لم تتقدم لأي وظيفة بعد",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
            is WorkerAppsUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
            is WorkerAppsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.applications, key = { it.id }) { app ->
                        ApplicationCard(
                            app = app,
                            isActionLoading = actionState is WorkerAppsActionState.Loading,
                            onWithdraw = { viewModel.withdraw(app.job, app.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    app: JobApplicationResponse,
    isActionLoading: Boolean,
    onWithdraw: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("سحب الطلب") },
            text  = { Text("هل تريد سحب طلبك لـ\"${app.jobTitle}\"؟") },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onWithdraw() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("سحب") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = app.jobTitle,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(app.status)
            }
            Text(
                text = "تاريخ التقديم: ${app.appliedAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (app.coverNote.isNotBlank()) {
                Text(
                    text = app.coverNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (app.status in listOf("PENDING", "OFFERED")) {
                OutlinedButton(
                    onClick = { showConfirm = true },
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("سحب الطلب") }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "PENDING"   -> "معلّق"   to MaterialTheme.colorScheme.secondary
        "OFFERED"   -> "معروض"   to MaterialTheme.colorScheme.primary
        "ACCEPTED"  -> "مقبول"   to MaterialTheme.colorScheme.tertiary
        "REJECTED"  -> "مرفوض"   to MaterialTheme.colorScheme.error
        "WITHDRAWN" -> "مسحوب"   to MaterialTheme.colorScheme.onSurfaceVariant
        else        -> status    to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
}
