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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.yawmiyati.app.data.api.AdminWorkerProfile
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AdminWorkersUiState
import com.yawmiyati.app.viewmodels.AdminWorkersViewModel
import com.yawmiyati.app.viewmodels.WorkerActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWorkersScreen(
    viewModel: AdminWorkersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var verifiedFilter by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(actionState) {
        if (actionState is WorkerActionState.Error) {
            snackbarHostState.showSnackbar((actionState as WorkerActionState.Error).message)
            viewModel.resetActionState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث (اسم / هاتف)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                trailingIcon = {
                    Button(onClick = { viewModel.loadWorkers(isVerified = verifiedFilter, search = searchQuery) }) {
                        Text("بحث")
                    }
                }
            )

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = verifiedFilter == null,
                        onClick = { verifiedFilter = null; viewModel.loadWorkers(null, searchQuery) },
                        label = { Text("الكل") }
                    )
                }
                item {
                    FilterChip(
                        selected = verifiedFilter == false,
                        onClick = { verifiedFilter = false; viewModel.loadWorkers(false, searchQuery) },
                        label = { Text("غير موثّق") }
                    )
                }
                item {
                    FilterChip(
                        selected = verifiedFilter == true,
                        onClick = { verifiedFilter = true; viewModel.loadWorkers(true, searchQuery) },
                        label = { Text("موثّق") }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            when (val state = uiState) {
                is AdminWorkersUiState.Loading -> LoadingIndicator()
                is AdminWorkersUiState.Empty   -> EmptyState(
                    message = "لا يوجد عمال مطابقون",
                    modifier = Modifier.fillMaxSize()
                )
                is AdminWorkersUiState.Error   -> ErrorMessage(
                    message = state.message,
                    onRetry = { viewModel.loadWorkers() }
                )
                is AdminWorkersUiState.Success -> {
                    val isActionLoading = actionState is WorkerActionState.Loading
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(state.workers, key = { it.id }) { worker ->
                            WorkerCard(
                                worker = worker,
                                isActionLoading = isActionLoading,
                                onVerify  = { viewModel.verifyWorker(worker.id) },
                                onSuspend = { viewModel.suspendWorker(worker.id) }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerCard(
    worker: AdminWorkerProfile,
    isActionLoading: Boolean,
    onVerify: () -> Unit,
    onSuspend: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(worker.fullName, style = MaterialTheme.typography.titleSmall)
                VerifiedBadge(worker.isVerified)
            }
            Text(
                worker.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (worker.locationCity.isNotBlank()) {
                Text(
                    worker.locationCity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            if (!worker.isVerified) {
                Button(
                    onClick = onVerify,
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("توثيق") }
            } else {
                OutlinedButton(
                    onClick = onSuspend,
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("إيقاف") }
            }
        }
    }
}

@Composable
private fun VerifiedBadge(isVerified: Boolean) {
    val (label, color) = if (isVerified)
        "موثّق" to MaterialTheme.colorScheme.primary
    else
        "غير موثّق" to MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
