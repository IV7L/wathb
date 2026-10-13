package com.yawmiyati.app.ui.screens.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.ui.components.JobMapMarker
import com.yawmiyati.app.ui.components.OsmMapView
import com.yawmiyati.app.ui.theme.WathbDeepTeal
import com.yawmiyati.app.ui.utils.cityLatLng
import com.yawmiyati.app.viewmodels.JobsUiState
import com.yawmiyati.app.viewmodels.JobsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val GAZA_LAT = 31.501
private const val GAZA_LNG = 34.468

@Suppress("MissingPermission")
private suspend fun awaitLocation(context: Context): Pair<Double, Double>? =
    withTimeoutOrNull(12_000L) {
        suspendCancellableCoroutine { cont ->
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

            // Try cached location first (immediate)
            for (p in providers) {
                if (!lm.isProviderEnabled(p)) continue
                val last = lm.getLastKnownLocation(p)
                if (last != null) {
                    cont.resume(Pair(last.latitude, last.longitude))
                    return@suspendCancellableCoroutine
                }
            }

            // Register one-shot listener
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(Pair(loc.latitude, loc.longitude))
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            }

            var registered = false
            for (p in providers) {
                if (lm.isProviderEnabled(p)) {
                    lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper())
                    registered = true
                    break
                }
            }

            if (!registered && cont.isActive) cont.resume(null)
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }

@Composable
fun WorkerMapScreen(
    onJobClick: (String) -> Unit,
    jobsViewModel: JobsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobsState by jobsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var workerLat by remember { mutableStateOf<Double?>(null) }
    var workerLng by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var selectedJobId by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scope.launch {
            isLocating = true
            val loc = awaitLocation(context)
            if (loc != null) { workerLat = loc.first; workerLng = loc.second }
            else snackbarHostState.showSnackbar("تعذّر تحديد موقعك — تأكد من تفعيل GPS")
            isLocating = false
        }
    }

    val doLocate: () -> Unit = {
        if (!isLocating) {
            scope.launch {
                val hasFine = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasFine) {
                    isLocating = true
                    val loc = awaitLocation(context)
                    if (loc != null) {
                        workerLat = loc.first
                        workerLng = loc.second
                    } else {
                        snackbarHostState.showSnackbar("تعذّر تحديد موقعك — تأكد من تفعيل GPS")
                    }
                    isLocating = false
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    val jobs = remember(jobsState) {
        (jobsState as? JobsUiState.Success)?.jobs ?: emptyList()
    }
    val jobMarkers = remember(jobs) {
        jobs.map { job ->
            val coords = cityLatLng(job.locationCity)
            JobMapMarker(id = job.id, lat = coords.lat, lng = coords.lng, title = job.title)
        }
    }
    val selectedJob = remember(selectedJobId, jobs) { jobs.find { it.id == selectedJobId } }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            OsmMapView(
                lat = workerLat ?: GAZA_LAT,
                lng = workerLng ?: GAZA_LNG,
                zoom = 11.0,
                interactive = true,
                showMarker = false,
                workerLat = workerLat,
                workerLng = workerLng,
                jobMarkers = jobMarkers,
                onJobMarkerClick = { id ->
                    selectedJobId = if (selectedJobId == id) null else id
                },
                modifier = Modifier.fillMaxSize()
            )

            // GPS locate button — top-end, pushed down from edge
            FloatingActionButton(
                onClick = doLocate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 12.dp)
                    .size(48.dp),
                containerColor = WathbDeepTeal,
                contentColor = Color.White
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Filled.GpsFixed, contentDescription = "تحديد موقعي")
                }
            }

            // Job info card
            selectedJob?.let { job ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = job.businessName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { selectedJobId = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onJobClick(job.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = WathbDeepTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("عرض تفاصيل الوظيفة")
                        }
                    }
                }
            }
        }
    }
}
