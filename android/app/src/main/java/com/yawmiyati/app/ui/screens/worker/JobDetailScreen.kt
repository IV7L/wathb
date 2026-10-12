package com.yawmiyati.app.ui.screens.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.data.api.OsrmClient
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
import org.osmdroid.util.GeoPoint

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

    // ── Location + routing state ───────────────────────────────────────────────
    val context = LocalContext.current
    var workerLat by remember { mutableStateOf<Double?>(null) }
    var workerLng by remember { mutableStateOf<Double?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeError by remember { mutableStateOf(false) }
    // Tracks whether we've tried to fetch location (avoids re-requesting on recomposition)
    var locationFetched by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tryGetLocation(context) { lat, lng ->
                workerLat = lat
                workerLng = lng
            }
        }
    }

    LaunchedEffect(Unit) {
        if (locationFetched) return@LaunchedEffect
        locationFetched = true
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (hasFine) {
            tryGetLocation(context) { lat, lng ->
                workerLat = lat
                workerLng = lng
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Fetch route once we have both worker location and job data
    val successState = jobState as? JobDetailUiState.Success
    LaunchedEffect(workerLat, workerLng, successState) {
        val wLat = workerLat ?: return@LaunchedEffect
        val wLng = workerLng ?: return@LaunchedEffect
        val job = successState?.job ?: return@LaunchedEffect
        if (routePoints.isNotEmpty()) return@LaunchedEffect // already fetched
        val jobCoords = cityLatLng(job.locationCity)
        try {
            val coords = "$wLng,$wLat;${jobCoords.lng},${jobCoords.lat}"
            val response = OsrmClient.service.getRoute(coords)
            routePoints = response.routes.firstOrNull()
                ?.geometry?.coordinates
                ?.map { GeoPoint(it[1], it[0]) }
                ?: emptyList()
        } catch (e: Exception) {
            routeError = true
        }
    }

    LaunchedEffect(jobId) { viewModel.loadJob(jobId) }

    LaunchedEffect(applyState) {
        when (val s = applyState) {
            is ApplyUiState.Applied -> {
                snackbarHostState.showSnackbar("تم تقديم طلبك بنجاح")
            }
            is ApplyUiState.AlreadyApplied -> {
                snackbarHostState.showSnackbar("لقد تقدّمت لهذه الوظيفة من قبل")
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
                        is ApplyUiState.Loading        -> ""
                        is ApplyUiState.Applied        -> "تم تقديم طلبك ✓"
                        is ApplyUiState.AlreadyApplied -> "تقدّمت لهذه الوظيفة من قبل"
                        else                            -> "تقدّم للوظيفة"
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
                workerLat = workerLat,
                workerLng = workerLng,
                routePoints = routePoints,
                routeError = routeError,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

private fun tryGetLocation(context: Context, onResult: (Double, Double) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            if (lm.isProviderEnabled(provider)) {
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) {
                    onResult(loc.latitude, loc.longitude)
                    return
                }
            }
        }
    } catch (_: SecurityException) {}
}

@Composable
private fun JobDetailContent(
    job: JobListingResponse,
    applyState: ApplyUiState,
    coverNote: String,
    onCoverNoteChange: (String) -> Unit,
    workerLat: Double?,
    workerLng: Double?,
    routePoints: List<GeoPoint>,
    routeError: Boolean,
    modifier: Modifier = Modifier
) {
    val canApply = applyState is ApplyUiState.Idle || applyState is ApplyUiState.Error
    val jobCoords = cityLatLng(job.locationCity)

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

        OsmMapView(
            lat = jobCoords.lat,
            lng = jobCoords.lng,
            zoom = 15.0,
            interactive = true,
            showMarker = true,
            workerLat = workerLat,
            workerLng = workerLng,
            routePoints = routePoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(MaterialTheme.shapes.medium)
        )

        if (routeError) {
            Text(
                text = "تعذّر تحميل المسار",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        Spacer(Modifier.height(80.dp))
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
