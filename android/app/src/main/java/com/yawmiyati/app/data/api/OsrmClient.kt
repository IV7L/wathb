package com.yawmiyati.app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OsrmClient {
    val service: OsrmService by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OsrmService::class.java)
    }
}
