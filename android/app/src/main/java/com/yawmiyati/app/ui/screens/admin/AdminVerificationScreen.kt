package com.yawmiyati.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.AdminBusinessProfile
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AdminUiState
import com.yawmiyati.app.viewmodels.AdminViewModel
import com.yawmiyati.app.viewmodels.VerifyActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(
    onLogout: () -> Unit,
    onWageReview: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogBusiness by remember { mutableStateOf<AdminBusinessProfile?>(null) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is VerifyActionState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetActionState()
                dialogBusiness = null
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مراجعة الأعمال التجارية") },
                actions = {
                    IconButton(onClick = onWageReview) {
                        Icon(Icons.Filled.Payments, contentDescription = "مراجعة الدفعات")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {
            // Status filter chips
            val filters = listOf(
                null to "الكل",
                "PENDING" to "معلق",
                "APPROVED" to "موافق",
                "REJECTED" to "مرفوض",
                "SUSPENDED" to "موقوف"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filters) { (value, label) ->
                    FilterChip(
                        selected = selectedFilter == value,
                        onClick = {
                            selectedFilter = value
                            viewModel.loadBusinesses(value)
                        },
                        label = { Text(label) }
                    )
                }
            }

            when (val state = uiState) {
                is AdminUiState.Loading -> LoadingIndicator()
                is AdminUiState.Error   -> ErrorMessage(
                    message = state.message,
                    onRetry = { viewModel.loadBusinesses(selectedFilter) }
                )
                is AdminUiState.Empty   -> {
                    Text(
                        text = "لا توجد نتائج",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is AdminUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        items(state.businesses, key = { it.id }) { business ->
                            BusinessCard(
                                business = business,
                                onClick = { dialogBusiness = business }
                            )
                        }
                    }
                }
            }
        }
    }

    // Verify/reject/suspend dialog
    dialogBusiness?.let { business ->
        VerifyDialog(
            business = business,
            isLoading = actionState is VerifyActionState.Loading,
            onDismiss = {
                if (actionState !is VerifyActionState.Loading) dialogBusiness = null
            },
            onConfirm = { action, reason ->
                viewModel.verifyBusiness(business.id, action, reason)
                dialogBusiness = null
            }
        )
    }
}

@Composable
private fun BusinessCard(business: AdminBusinessProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = business.legalName,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(business.verificationStatus)
            }
            Text(
                text = business.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!business.locationCity.isNullOrBlank()) {
                Text(
                    text = business.locationCity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val label = when (status) {
        "PENDING"   -> "معلق"
        "APPROVED"  -> "موافق"
        "REJECTED"  -> "مرفوض"
        "SUSPENDED" -> "موقوف"
        else        -> "حالة غير معروفة"
    }
    val color = when (status) {
        "APPROVED"  -> MaterialTheme.colorScheme.primary
        "REJECTED"  -> MaterialTheme.colorScheme.error
        "SUSPENDED" -> MaterialTheme.colorScheme.tertiary
        else        -> MaterialTheme.colorScheme.secondary
    }
    Badge(containerColor = color) { Text(label) }
}

@Composable
private fun VerifyDialog(
    business: AdminBusinessProfile,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (action: String, reason: String) -> Unit
) {
    var selectedAction by rememberSaveable { mutableStateOf("approve") }
    var reason by rememberSaveable { mutableStateOf("") }

    val needsReason = selectedAction in listOf("reject", "suspend")
    val canConfirm = !isLoading && (!needsReason || reason.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(business.legalName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الحالة الحالية: ${statusLabel(business.verificationStatus)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = business.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))
                Text("اختر الإجراء:", style = MaterialTheme.typography.labelLarge)

                listOf(
                    "approve" to "موافقة على الحساب",
                    "reject"  to "رفض الحساب",
                    "suspend" to "إيقاف الحساب"
                ).forEach { (action, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedAction = action }
                    ) {
                        RadioButton(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (needsReason) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("السبب (مطلوب)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onConfirm(selectedAction, reason) },
                enabled = canConfirm
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("إلغاء")
            }
        }
    )
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING"   -> "معلق"
    "APPROVED"  -> "موافق عليه"
    "REJECTED"  -> "مرفوض"
    "SUSPENDED" -> "موقوف"
    else        -> "حالة غير معروفة"
}
