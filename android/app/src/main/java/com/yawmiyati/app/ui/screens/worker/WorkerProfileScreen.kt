package com.yawmiyati.app.ui.screens.worker

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.WorkerProfileSaveState
import com.yawmiyati.app.viewmodels.WorkerProfileUiState
import com.yawmiyati.app.viewmodels.WorkerProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    onBack: (() -> Unit)? = null,
    onLockApp: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    viewModel: WorkerProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveState) {
        when (saveState) {
            is WorkerProfileSaveState.Saved -> {
                snackbarHostState.showSnackbar("تم حفظ الملف الشخصي")
                viewModel.resetSaveState()
            }
            is WorkerProfileSaveState.Error -> {
                snackbarHostState.showSnackbar((saveState as WorkerProfileSaveState.Error).message)
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملفي الشخصي") },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is WorkerProfileUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is WorkerProfileUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
            is WorkerProfileUiState.Success -> {
                val profile = state.profile
                var fullName       by remember(profile) { mutableStateOf(profile.fullName) }
                var locationCity   by remember(profile) { mutableStateOf(profile.locationCity) }
                var locationRegion by remember(profile) { mutableStateOf(profile.locationRegion) }
                var isAvailable    by remember(profile) { mutableStateOf(profile.isAvailable) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Read-only info
                    Text(
                        text = "الهاتف: ${profile.phone}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.isVerified) {
                        Text(
                            text = "موثّق ✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم الكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = locationCity,
                        onValueChange = { locationCity = it },
                        label = { Text("المدينة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = locationRegion,
                        onValueChange = { locationRegion = it },
                        label = { Text("المنطقة / الحي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("متاح للعمل", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.save(fullName, locationCity, locationRegion, isAvailable) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = saveState !is WorkerProfileSaveState.Saving,
                    ) {
                        Text(if (saveState is WorkerProfileSaveState.Saving) "جارٍ الحفظ..." else "حفظ")
                    }

                    if (onLockApp != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onLockApp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("قفل التطبيق")
                        }
                    }

                    if (onLogout != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("تسجيل الخروج")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
