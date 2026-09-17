package com.example.muslimvn.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
    suspend fun getAddress(lat: Double, lng: Double): String?
}
