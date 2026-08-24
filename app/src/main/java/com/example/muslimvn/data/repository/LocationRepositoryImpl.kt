package com.example.muslimvn.data.repository

import android.annotation.SuppressLint
import android.location.Location
import com.example.muslimvn.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationProviderClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_LATITUDE = 10.7005
        const val DEFAULT_LONGITUDE = 105.1147
    }
}
