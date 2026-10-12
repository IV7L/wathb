package com.yawmiyati.app.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    DisposableEffect(lat, lng, showMarker, workerLat, workerLng, routePoints.size) {
        mapView.overlays.clear()

        // Route polyline (drawn first so markers appear on top)
        if (routePoints.isNotEmpty()) {
            val polyline = Polyline().apply {
                setPoints(routePoints)
                outlinePaint.color = AndroidColor.parseColor("#0EA5A3")
                outlinePaint.strokeWidth = 10f
            }
            mapView.overlays.add(polyline)
        }

        // Job location marker (Deep Teal pin)
        if (showMarker) {
            val pinIcon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = pinIcon
                title = "موقع الوظيفة"
            }
            mapView.overlays.add(marker)
        }

        // Worker location marker (Teal pin)
        if (workerLat != null && workerLng != null) {
            val workerIcon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin_worker)
            val workerMarker = Marker(mapView).apply {
                position = GeoPoint(workerLat, workerLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = workerIcon
                title = "موقعك"
            }
            mapView.overlays.add(workerMarker)
        }

        // Auto-zoom to show the full route when available
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

    AndroidView(factory = { mapView }, modifier = modifier)
}
