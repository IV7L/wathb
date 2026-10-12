package com.yawmiyati.app.ui.screens.admin

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.DisputeResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AdminDisputesViewModel
import com.yawmiyati.app.viewmodels.DisputeActionState
import com.yawmiyati.app.viewmodels.DisputesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDisputesScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AdminDisputesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var resolveDialog by remember { mutableStateOf<Pair<DisputeResponse, String>?>(null) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is DisputeActionState.Success -> {
                snackbarHostState.showSnackbar("تم تحديث النزاع بنجاح")
                viewModel.resetActionState()
                resolveDialog = null
            }
            is DisputeActionState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    resolveDialog?.let { (dispute, action) ->
        ResolveDialog(
            dispute = dispute,
            action = action,
            isLoading = actionState is DisputeActionState.Loading,
            onDismiss = { resolveDialog = null },
            onConfirm = { notes -> viewModel.resolve(dispute.id, action, notes) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نزاعات الأجور") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is DisputesUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is DisputesUiState.Empty -> EmptyState(
                message = "لا توجد نزاعات أجور مفتوحة",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
            is DisputesUiState.Error -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
            is DisputesUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.disputes, key = { it.id }) { dispute ->
                        DisputeCard(
                            dispute = dispute,
                            isActionLoading = actionState is DisputeActionState.Loading,
                            onPaid = { resolveDialog = dispute to "paid" },
                            onNotPaid = { resolveDialog = dispute to "not_paid" }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DisputeCard(
    dispute: DisputeResponse,
    isActionLoading: Boolean,
    onPaid: () -> Unit,
    onNotPaid: () -> Unit,
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
                Text(
                    text = dispute.raisedByPhone,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                DisputeStatusBadge(dispute.status)
            }
            Text(
                text = dispute.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "تاريخ الرفع: ${dispute.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!dispute.resolutionNotes.isNullOrBlank()) {
                Text(
                    text = "ملاحظات: ${dispute.resolutionNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dispute.status in listOf("OPEN", "IN_REVIEW")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPaid,
                        enabled = !isActionLoading,
                        modifier = Modifier.weight(1f)
                    ) { Text("دُفع") }
                    OutlinedButton(
                        onClick = onNotPaid,
                        enabled = !isActionLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("لم يُدفع") }
                }
            }
        }
    }
}

@Composable
private fun ResolveDialog(
    dispute: DisputeResponse,
    action: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var notes by rememberSaveable { mutableStateOf("") }
    val actionLabel = if (action == "paid") "دُفع" else "لم يُدفع"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد: $actionLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المبلّغ: ${dispute.raisedByPhone}")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (مطلوبة)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                enabled = notes.isNotBlank() && !isLoading
            ) { Text("تأكيد") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun DisputeStatusBadge(status: String) {
    val (label, color) = when (status) {
        "OPEN"      -> "مفتوح"    to MaterialTheme.colorScheme.error
        "IN_REVIEW" -> "قيد المراجعة" to MaterialTheme.colorScheme.secondary
        "RESOLVED"  -> "محلول"    to MaterialTheme.colorScheme.tertiary
        "CLOSED"    -> "مغلق"     to MaterialTheme.colorScheme.onSurfaceVariant
        else        -> status     to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            text = label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
