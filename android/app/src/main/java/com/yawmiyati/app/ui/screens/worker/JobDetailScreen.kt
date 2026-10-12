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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.ui.components.OsmMapView
import com.yawmiyati.app.ui.utils.cityLatLng
import com.yawmiyati.app.ui.utils.formatArabicDate
import com.yawmiyati.app.ui.utils.formatArabicTime
import com.yawmiyati.app.ui.utils.formatArabicWage
import com.yawmiyati.app.ui.utils.toArabic
import com.yawmiyati.app.viewmodels.ApplyUiState
import com.yawmiyati.app.viewmodels.JobDetailUiState
import com.yawmiyati.app.viewmodels.JobDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: JobDetailViewModel = viewModel()
) {
    val jobState by viewModel.jobState.collectAsState()
    val applyState by viewModel.applyState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Cover note is kept in saveable state so it survives re-compositions.
    var coverNote by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(jobId) { viewModel.loadJob(jobId) }

    LaunchedEffect(applyState) {
        when (val s = applyState) {
            is ApplyUiState.Applied -> {
                snackbarHostState.showSnackbar("تم تقديم طلبك بنجاح")
                // Do not reset — Applied state persists to keep the button disabled.
            }
            is ApplyUiState.AlreadyApplied -> {
                snackbarHostState.showSnackbar("لقد تقدّمت لهذه الوظيفة من قبل")
                // Do not reset — AlreadyApplied state persists.
            }
            is ApplyUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetApplyState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الوظيفة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (jobState is JobDetailUiState.Success) {
                val job = (jobState as JobDetailUiState.Success).job
                if (job.status == "PUBLISHED") {
                    val buttonEnabled = applyState is ApplyUiState.Idle ||
                        applyState is ApplyUiState.Error
                    val buttonLabel = when (applyState) {
                        is ApplyUiState.Loading       -> ""
                        is ApplyUiState.Applied       -> "تم تقديم طلبك ✓"
                        is ApplyUiState.AlreadyApplied -> "تقدّمت لهذه الوظيفة من قبل"
                        else                           -> "تقدّم للوظيفة"
                    }
                    Button(
                        onClick = { viewModel.applyToJob(jobId, coverNote) },
                        enabled = buttonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (applyState is ApplyUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = jobState) {
            is JobDetailUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is JobDetailUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.loadJob(jobId) },
                modifier = Modifier.padding(padding)
            )
            is JobDetailUiState.Success -> JobDetailContent(
                job = state.job,
                applyState = applyState,
                coverNote = coverNote,
                onCoverNoteChange = { coverNote = it },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun JobDetailContent(
    job: JobListingResponse,
    applyState: ApplyUiState,
    coverNote: String,
    onCoverNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val canApply = applyState is ApplyUiState.Idle || applyState is ApplyUiState.Error

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = job.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = job.businessName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        val mapCoords = cityLatLng(job.locationCity)
        OsmMapView(
            lat = mapCoords.lat,
            lng = mapCoords.lng,
            zoom = 15.0,
            interactive = true,
            showMarker = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(Modifier.height(4.dp))
        DetailRow(label = "الموقع", value = "${job.locationCity} — ${job.locationAddress}")
        DetailRow(label = "تاريخ العمل", value = formatArabicDate(job.workDate))
        DetailRow(label = "الوقت", value = "${formatArabicTime(job.startTime)} – ${formatArabicTime(job.endTime)}")
        DetailRow(label = "عدد العمال المطلوبين", value = job.numWorkersNeeded.toArabic())

        HorizontalDivider()

        DetailRow(label = "الأجر", value = formatArabicWage(job.wageAmount, job.wageCurrency, job.wageBasis))
        DetailRow(label = "طريقة الدفع", value = paymentMethodLabel(job.paymentMethod))
        DetailRow(label = "مدة سداد الأجر", value = "${job.paymentDeadlineDays.toArabic()} أيام بعد الإنجاز")

        if (job.description.isNotBlank()) {
            HorizontalDivider()
            Text(text = "وصف الوظيفة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = job.description, style = MaterialTheme.typography.bodyMedium)
        }

        if (job.requiredSkills.isNotBlank()) {
            HorizontalDivider()
            Text(text = "المهارات المطلوبة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = job.requiredSkills, style = MaterialTheme.typography.bodyMedium)
        }

        if (job.attendanceRequirements.isNotBlank()) {
            HorizontalDivider()
            Text(text = "اشتراطات الحضور", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = job.attendanceRequirements, style = MaterialTheme.typography.bodyMedium)
        }

        if (job.isFunded) {
            HorizontalDivider()
            Text(
                text = "هذه الوظيفة ممولة من خلال صندوق دعم العمال",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Cover note — only shown when the worker can still submit an application.
        if (job.status == "PUBLISHED" && canApply) {
            HorizontalDivider()
            Text(
                text = "ملاحظة التقديم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = coverNote,
                onValueChange = onCoverNoteChange,
                label = { Text("اختياري — اكتب ملاحظة للصاحب العمل") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }

        Spacer(Modifier.height(80.dp)) // space for bottom bar
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.55f)
        )
    }
}

private fun paymentMethodLabel(method: String): String = when (method) {
    "CASH"          -> "نقداً"
    "BANK_TRANSFER" -> "تحويل بنكي"
    "OTHER"         -> "طريقة أخرى"
    else            -> method
}
