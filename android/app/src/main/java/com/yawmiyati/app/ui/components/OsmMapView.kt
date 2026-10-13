package com.yawmiyati.app.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yawmiyati.app.R
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class JobMapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String = ""
)

@Composable
fun OsmMapView(
    lat: Double,
    lng: Double,
    zoom: Double = 14.0,
    interactive: Boolean = false,
    showMarker: Boolean = true,
    workerLat: Double? = null,
    workerLng: Double? = null,
    routePoints: List<GeoPoint> = emptyList(),
    jobMarkers: List<JobMapMarker> = emptyList(),
    onJobMarkerClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnClick by rememberUpdatedState(onJobMarkerClick)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            isClickable = interactive
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(lat, lng))
            if (!interactive) {
                setOnTouchListener { _, _ -> true }
            }
        }
    }

    // Re-center map when lat/lng changes (e.g. worker location arrives for map tab)
    LaunchedEffect(lat, lng) {
        mapView.controller.setCenter(GeoPoint(lat, lng))
    }

    DisposableEffect(showMarker, workerLat, workerLng, routePoints.size, jobMarkers.size) {
        mapView.overlays.clear()

        // 1. Route polyline (bottom layer)
        if (routePoints.isNotEmpty()) {
            val polyline = Polyline().apply {
                setPoints(routePoints)
                outlinePaint.color = AndroidColor.parseColor("#0EA5A3")
                outlinePaint.strokeWidth = 10f
            }
            mapView.overlays.add(polyline)
        }

        // 2. Job markers (middle layer — teal pins, clickable)
        for (jm in jobMarkers) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(jm.lat, jm.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
                title = jm.title
                setOnMarkerClickListener { _, _ ->
                    currentOnClick(jm.id)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // 3. Single job marker for job detail screen (above job markers)
        if (showMarker) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
                title = "موقع الوظيفة"
            }
            mapView.overlays.add(marker)
        }

        // 4. Worker marker — drawn LAST so it's always on top
        if (workerLat != null && workerLng != null) {
            val workerMarker = Marker(mapView).apply {
                position = GeoPoint(workerLat, workerLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin_worker)
                title = "موقعك"
            }
            mapView.overlays.add(workerMarker)
        }

        // Auto-zoom to show full route when available
        if (routePoints.size >= 2) {
            val box = BoundingBox.fromGeoPoints(routePoints)
            mapView.post { mapView.zoomToBoundingBox(box.increaseByScale(1.3f), true) }
        }

        mapView.invalidate()
        onDispose { }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Text(
            text = "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color(0xFF333333),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 2.dp)
        )
    }
}
