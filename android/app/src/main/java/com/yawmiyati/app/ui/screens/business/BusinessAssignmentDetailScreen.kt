package com.yawmiyati.app.ui.screens.business

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.ui.components.ConfirmCancelDialog
import com.yawmiyati.app.viewmodels.AssignmentWorkActionState
import com.yawmiyati.app.viewmodels.BusinessAssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAssignmentDetailScreen(
    assignmentId: String,
    workerPhone: String,
    assignmentStatus: String = "",
    onBack: () -> Unit,
    viewModel: BusinessAssignmentViewModel = viewModel()
) {
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }
    var showCompleteConfirm by remember { mutableStateOf(false) }
    var currentStatus by remember { mutableStateOf(assignmentStatus) }

    LaunchedEffect(assignmentId) { viewModel.load(assignmentId) }

    val isActionLoading = actionState is AssignmentWorkActionState.Loading

    LaunchedEffect(actionState) {
        when (actionState) {
            is AssignmentWorkActionState.Success -> {
                showCancelDialog = false
                showCompleteConfirm = false
                if (currentStatus in listOf("ACCEPTED", "ACTIVE")) currentStatus = "COMPLETED"
                snackbarHostState.showSnackbar("تم بنجاح")
                viewModel.resetActionState()
            }
            is AssignmentWorkActionState.Error -> {
                showCancelDialog = false
                showCompleteConfirm = false
                snackbarHostState.showSnackbar((actionState as AssignmentWorkActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    if (showCancelDialog) {
        ConfirmCancelDialog(
            title = "إلغاء المهمة",
            body = "هل أنت متأكد أنك تريد إلغاء مهمة هذا العامل؟",
            isLoading = isActionLoading,
            onConfirm = { reason -> viewModel.cancelAssignment(assignmentId, reason) },
            onDismiss = { showCancelDialog = false }
        )
    }

    if (showCompleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirm = false },
            title = { Text("تأكيد إتمام العمل") },
            text = { Text("سيتم تأكيد إتمام العمل وإرسال إشعار للعامل لإقرار استلام أجره.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.businessComplete(assignmentId) },
                    enabled = !isActionLoading
                ) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المهمة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Worker contact card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "العامل",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(workerPhone, style = MaterialTheme.typography.titleMedium)
                    if (workerPhone.isNotBlank()) {
                        val localPhone = if (workerPhone.startsWith("+970"))
                            "0" + workerPhone.removePrefix("+970")
                        else workerPhone
                        val waPhone = workerPhone.replace("+", "")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$localPhone"))
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("اتصال") }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waPhone"))
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("واتساب") }
                        }
                    }
                }
            }

            // Status + action card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "حالة المهمة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    when (currentStatus) {
                        "OFFERED" -> {
                            Text("بانتظار قبول العامل", style = MaterialTheme.typography.bodyLarge)
                        }
                        "ACCEPTED", "ACTIVE" -> {
                            Button(
                                onClick = { showCompleteConfirm = true },
                                enabled = !isActionLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isActionLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("تأكيد إتمام العمل")
                                }
                            }
                        }
                        "COMPLETED" -> {
                            Text(
                                "✓ تم تأكيد إتمام العمل",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "بانتظار إقرار العامل على استلام أجره",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "CANCELLED" -> {
                            Text(
                                "ملغاة",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text(
                                "✓ تم",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Cancel button (OFFERED or ACCEPTED only)
            if (currentStatus in listOf("OFFERED", "ACCEPTED")) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) { Text("إلغاء المهمة") }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
