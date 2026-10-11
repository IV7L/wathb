package com.yawmiyati.app.ui.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.viewmodels.JobCreateUiState
import com.yawmiyati.app.viewmodels.JobCreateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCreateScreen(
    onJobCreated: () -> Unit,
    onBack: () -> Unit,
    viewModel: JobCreateViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("") }
    var locationCity by remember { mutableStateOf("") }
    var workDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var numWorkers by remember { mutableStateOf("1") }
    var wageAmount by remember { mutableStateOf("") }
    var wageBasis by remember { mutableStateOf("PER_DAY") }
    var paymentMethod by remember { mutableStateOf("CASH") }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is JobCreateUiState.Success -> {
                viewModel.reset()
                onJobCreated()
            }
            is JobCreateUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.reset()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نشر وظيفة جديدة") },
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
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان الوظيفة") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("وصف الوظيفة") }, minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = locationCity, onValueChange = { locationCity = it },
                label = { Text("المدينة") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = locationAddress, onValueChange = { locationAddress = it },
                label = { Text("العنوان التفصيلي") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = workDate, onValueChange = { workDate = it },
                label = { Text("تاريخ العمل (YYYY-MM-DD)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime, onValueChange = { startTime = it },
                    label = { Text("وقت البداية (HH:MM)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTime, onValueChange = { endTime = it },
                    label = { Text("وقت النهاية (HH:MM)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = numWorkers, onValueChange = { numWorkers = it.filter { c -> c.isDigit() } },
                label = { Text("عدد العمال المطلوبين") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = wageAmount, onValueChange = { wageAmount = it.filter { c -> c.isDigit() } },
                label = { Text("الأجر (بالشيكل الكامل)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // Wage basis selector
            Text("أساس الأجر", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PER_DAY" to "يومي", "PER_HOUR" to "بالساعة", "PER_TASK" to "بالمهمة").forEach { (code, label) ->
                    FilterChip(
                        selected = wageBasis == code,
                        onClick = { wageBasis = code },
                        label = { Text(label) }
                    )
                }
            }

            // Payment method selector
            Text("طريقة الدفع", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("CASH" to "نقداً", "BANK_TRANSFER" to "تحويل بنكي").forEach { (code, label) ->
                    FilterChip(
                        selected = paymentMethod == code,
                        onClick = { paymentMethod = code },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.createJob(
                        title = title,
                        description = description,
                        locationAddress = locationAddress,
                        locationCity = locationCity,
                        workDate = workDate,
                        startTime = startTime,
                        endTime = endTime,
                        numWorkers = numWorkers.toIntOrNull() ?: 1,
                        wageAmountIls = wageAmount.toLongOrNull() ?: 0L,
                        wageBasis = wageBasis,
                        paymentMethod = paymentMethod,
                        paymentDeadlineDays = 3
                    )
                },
                enabled = uiState !is JobCreateUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is JobCreateUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("نشر الوظيفة", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
