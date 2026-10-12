package com.yawmiyati.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface OsrmService {
    @GET("route/v1/driving/{coordinates}?overview=full&geometries=geojson")
    suspend fun getRoute(
        @Path("coordinates", encoded = true) coordinates: String
    ): OsrmRouteResponse
}

data class OsrmRouteResponse(
    val routes: List<OsrmRoute>
)

data class OsrmRoute(
    val geometry: OsrmGeometry,
    val distance: Double,
    val duration: Double
)

data class OsrmGeometry(
    val coordinates: List<List<Double>>
)
