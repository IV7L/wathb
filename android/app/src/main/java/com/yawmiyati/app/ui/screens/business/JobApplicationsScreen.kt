package com.yawmiyati.app.ui.screens.business

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobApplicationResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.ApplicationActionState
import com.yawmiyati.app.viewmodels.ApplicationsUiState
import com.yawmiyati.app.viewmodels.JobApplicationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationsScreen(
    jobId: String,
    onBack: () -> Unit,
    onManageAssignment: (assignmentId: String, workerPhone: String) -> Unit = { _, _ -> },
    viewModel: JobApplicationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(jobId) { viewModel.loadApplications(jobId) }

    LaunchedEffect(actionState) {
        if (actionState is ApplicationActionState.Error) {
            snackbarHostState.showSnackbar((actionState as ApplicationActionState.Error).message)
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طلبات التوظيف") },
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
            is ApplicationsUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is ApplicationsUiState.Empty   -> EmptyState(
                message = "لا توجد طلبات لهذه الوظيفة بعد",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
            is ApplicationsUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.loadApplications(jobId) },
                modifier = Modifier.padding(padding)
            )
            is ApplicationsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.applications, key = { it.id }) { app ->
                        ApplicationCard(
                            application = app,
                            isActionLoading = actionState is ApplicationActionState.Loading,
                            onOffer  = { viewModel.offerApplication(app.id) },
                            onReject = { viewModel.rejectApplication(app.id) },
                            onManageAssignment = {
                                val assignId = app.assignmentId
                                if (assignId != null) onManageAssignment(assignId, app.workerPhone ?: "")
                            }
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
    application: JobApplicationResponse,
    isActionLoading: Boolean,
    onOffer: () -> Unit,
    onReject: () -> Unit,
    onManageAssignment: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!application.workerName.isNullOrBlank()) {
                            Text(
                                text = application.workerName,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        VerificationBadge(application.workerIsVerified)
                    }
                    if (!application.workerPhone.isNullOrBlank()) {
                        Text(
                            text = application.workerPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ApplicationStatusChip(application.status)
            }

            if (application.coverNote.isNotBlank()) {
                Text(
                    text = application.coverNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "تاريخ التقديم: ${application.appliedAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (application.status == "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOffer,
                        enabled = !isActionLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("عرض الوظيفة")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isActionLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("رفض")
                    }
                }
            } else if (application.status == "OFFERED") {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isActionLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("سحب العرض")
                }
            } else if (application.status == "ACCEPTED" && application.assignmentId != null) {
                Button(
                    onClick = onManageAssignment,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إدارة المهمة والحضور")
                }
            }
        }
    }
}

@Composable
private fun VerificationBadge(isVerified: Boolean) {
    val (label, containerColor, contentColor) = if (isVerified) {
        Triple("موثّق", Color(0xFF2E7D32), Color.White)
    } else {
        Triple("غير موثّق", MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ApplicationStatusChip(status: String) {
    val (label, color) = when (status) {
        "PENDING"   -> "معلق"    to MaterialTheme.colorScheme.secondary
        "OFFERED"   -> "عُرض عليه" to MaterialTheme.colorScheme.primary
        "ACCEPTED"  -> "قبل"      to MaterialTheme.colorScheme.tertiary
        "REJECTED"  -> "مرفوض"   to MaterialTheme.colorScheme.error
        "WITHDRAWN" -> "انسحب"   to MaterialTheme.colorScheme.onSurfaceVariant
        else        -> "حالة غير معروفة" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
