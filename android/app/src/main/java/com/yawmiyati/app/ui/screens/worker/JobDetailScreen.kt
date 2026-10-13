package com.yawmiyati.app.ui.screens.worker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.ui.components.OsmMapView
import com.yawmiyati.app.ui.theme.WathbDeepTeal
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
    var coverNote by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(jobId) { viewModel.loadJob(jobId) }

    LaunchedEffect(applyState) {
        when (val s = applyState) {
            is ApplyUiState.Applied        -> snackbarHostState.showSnackbar("تم تقديم طلبك بنجاح")
            is ApplyUiState.AlreadyApplied -> snackbarHostState.showSnackbar("لقد تقدّمت لهذه الوظيفة من قبل")
            is ApplyUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetApplyState()
            }
            else -> {}
        }
    }

    val successState = jobState as? JobDetailUiState.Success
    val job = successState?.job
    val isPublished = job?.status == "PUBLISHED"
    val canApply = applyState is ApplyUiState.Idle || applyState is ApplyUiState.Error
    val buttonLabel = when (applyState) {
        is ApplyUiState.Loading        -> ""
        is ApplyUiState.Applied        -> "تم تقديم طلبك ✓"
        is ApplyUiState.AlreadyApplied -> "تقدّمت لهذه الوظيفة من قبل"
        else                            -> "تقدّم للوظيفة"
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (val state = jobState) {
                    is JobDetailUiState.Loading -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                    is JobDetailUiState.Error   -> ErrorMessage(
                        message = state.message,
                        onRetry = { viewModel.loadJob(jobId) },
                        modifier = Modifier.fillMaxSize()
                    )
                    is JobDetailUiState.Success -> JobDetailContent(
                        job = state.job,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isPublished) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canApply) {
                            OutlinedTextField(
                                value = coverNote,
                                onValueChange = { coverNote = it },
                                label = { Text("ملاحظة للصاحب العمل (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Button(
                            onClick = { viewModel.applyToJob(jobId, coverNote) },
                            enabled = canApply,
                            colors = ButtonDefaults.buttonColors(containerColor = WathbDeepTeal),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (applyState is ApplyUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobDetailContent(
    job: JobListingResponse,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val jobCoords = cityLatLng(job.locationCity)

    Column(
        modifier = modifier
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

        OsmMapView(
            lat = jobCoords.lat,
            lng = jobCoords.lng,
            zoom = 15.0,
            interactive = true,
            showMarker = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium)
        )

        OutlinedButton(
            onClick = {
                val geoUri = Uri.parse(
                    "geo:${jobCoords.lat},${jobCoords.lng}?q=${jobCoords.lat},${jobCoords.lng}(${Uri.encode(job.title)})"
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("افتح في الخرائط")
        }

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

        Spacer(Modifier.height(8.dp))
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
