package com.yawmiyati.app.ui.screens.worker

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedTextField
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
import com.yawmiyati.app.viewmodels.WorkerAssignmentActionState
import com.yawmiyati.app.viewmodels.WorkerAssignmentDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerAssignmentDetailScreen(
    assignmentId: String,
    workerPhone: String = "",
    businessPhone: String = "",
    assignmentStatus: String = "",
    onBack: () -> Unit,
    viewModel: WorkerAssignmentDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }

    val wages by viewModel.wagesForAssignment.collectAsState()

    LaunchedEffect(assignmentId) { viewModel.load(assignmentId) }

    val isLoading = actionState is WorkerAssignmentActionState.Loading

    LaunchedEffect(actionState) {
        when (actionState) {
            is WorkerAssignmentActionState.Success -> {
                showCancelDialog = false
                showDisputeDialog = false
                snackbarHostState.showSnackbar("تم بنجاح")
                viewModel.resetActionState()
            }
            is WorkerAssignmentActionState.Error -> {
                showCancelDialog = false
                showDisputeDialog = false
                snackbarHostState.showSnackbar((actionState as WorkerAssignmentActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    if (showCancelDialog) {
        ConfirmCancelDialog(
            title = "إلغاء المهمة",
            body = "هل أنت متأكد أنك تريد إلغاء هذه المهمة؟",
            isLoading = isLoading,
            onConfirm = { reason -> viewModel.cancelAssignment(assignmentId, reason) },
            onDismiss = { showCancelDialog = false }
        )
    }

    if (showDisputeDialog) {
        AlertDialog(
            onDismissRequest = { showDisputeDialog = false },
            title = { Text("الاعتراض على الأجر") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اشرح سبب الاعتراض:")
                    OutlinedTextField(
                        value = disputeReason,
                        onValueChange = { disputeReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        wages?.let { viewModel.disputeWage(it.id, disputeReason) }
                    },
                    enabled = disputeReason.isNotBlank() && !isLoading
                ) { Text("تأكيد الاعتراض") }
            },
            dismissButton = {
                TextButton(onClick = { showDisputeDialog = false }) { Text("إلغاء") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل المهمة") },
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
            // Business contact card
            if (businessPhone.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "صاحب العمل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(businessPhone, style = MaterialTheme.typography.titleMedium)
                        val localPhone = if (businessPhone.startsWith("+970"))
                            "0" + businessPhone.removePrefix("+970")
                        else businessPhone
                        val waPhone = businessPhone.replace("+", "")
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

            // Status / wage confirmation card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "حالة المهمة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    when (assignmentStatus) {
                        "OFFERED" -> {
                            Text("عرض مهمة — بانتظار قرارك", style = MaterialTheme.typography.bodyLarge)
                        }
                        "ACCEPTED", "ACTIVE" -> {
                            Text(
                                "مقبولة — في انتظار يوم العمل",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        "COMPLETED" -> {
                            val wage = wages
                            if (wage != null && wage.status == "AWAITING_ACK") {
                                Text("تأكيد استلام الأجر", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    wage.amountDisplay,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val methodLabel = when (wage.declarations.firstOrNull()?.paymentMethod) {
                                    "CASH" -> "الدفع نقداً"
                                    "BANK_TRANSFER" -> "تحويل بنكي"
                                    else -> ""
                                }
                                if (methodLabel.isNotBlank()) {
                                    Text(
                                        methodLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.acknowledgeWage(wage.id) },
                                        enabled = !isLoading,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("تأكيد الاستلام") }
                                    OutlinedButton(
                                        onClick = { showDisputeDialog = true },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("اعتراض") }
                                }
                            } else if (wage != null && wage.status in listOf("VERIFIED_PAID", "DECLARED")) {
                                Text(
                                    "✓ تم دفع أجرك",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            } else {
                                Text(
                                    "جاري تحميل بيانات الأجر...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        "CANCELLED" -> {
                            Text(
                                "ملغاة",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        "DECLINED" -> {
                            Text(
                                "مرفوضة",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                "✓ تم دفع أجرك",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Cancel button (ACCEPTED only)
            if (assignmentStatus == "ACCEPTED") {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    enabled = !isLoading,
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
