package com.yawmiyati.app.ui.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.BusinessProfileSaveState
import com.yawmiyati.app.viewmodels.BusinessProfileUiState
import com.yawmiyati.app.viewmodels.BusinessProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    onBack: () -> Unit,
    viewModel: BusinessProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveState) {
        when (saveState) {
            is BusinessProfileSaveState.Saved -> {
                snackbarHostState.showSnackbar("تم حفظ ملف المنشأة")
                viewModel.resetSaveState()
            }
            is BusinessProfileSaveState.Error -> {
                snackbarHostState.showSnackbar((saveState as BusinessProfileSaveState.Error).message)
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملف المنشأة") },
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
            is BusinessProfileUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is BusinessProfileUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
            is BusinessProfileUiState.Success -> {
                val profile = state.profile
                var legalName       by remember(profile) { mutableStateOf(profile.legalName) }
                var tradeName       by remember(profile) { mutableStateOf(profile.tradeName) }
                var locationAddress by remember(profile) { mutableStateOf(profile.locationAddress) }
                var locationCity    by remember(profile) { mutableStateOf(profile.locationCity) }
                var contactName     by remember(profile) { mutableStateOf(profile.contactName) }
                var contactPhone    by remember(profile) { mutableStateOf(profile.contactPhone) }

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
                    if (profile.phone.isNotBlank()) {
                        Text(
                            text = "الهاتف: ${profile.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!profile.email.isNullOrBlank()) {
                        Text(
                            text = "البريد: ${profile.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val verStatus = when (profile.verificationStatus) {
                        "APPROVED"  -> "موثّق ✓" to MaterialTheme.colorScheme.primary
                        "PENDING"   -> "في انتظار التحقق" to MaterialTheme.colorScheme.secondary
                        "REJECTED"  -> "مرفوض" to MaterialTheme.colorScheme.error
                        "SUSPENDED" -> "موقوف" to MaterialTheme.colorScheme.error
                        else        -> profile.verificationStatus to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = verStatus.second.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            verStatus.first,
                            style = MaterialTheme.typography.labelMedium,
                            color = verStatus.second,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = legalName,
                        onValueChange = { legalName = it },
                        label = { Text("الاسم القانوني") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = tradeName,
                        onValueChange = { tradeName = it },
                        label = { Text("الاسم التجاري") },
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
                        value = locationAddress,
                        onValueChange = { locationAddress = it },
                        label = { Text("العنوان") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("اسم المسؤول") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("هاتف التواصل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.save(
                                legalName, tradeName, locationAddress,
                                locationCity, contactName, contactPhone
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = saveState !is BusinessProfileSaveState.Saving,
                    ) {
                        Text(if (saveState is BusinessProfileSaveState.Saving) "جارٍ الحفظ..." else "حفظ")
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
