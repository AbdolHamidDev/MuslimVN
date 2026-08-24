package com.example.muslimvn.domain.util

import kotlin.math.*

object QiblaUtils {
    const val MECCA_LATITUDE = 21.4225
    const val MECCA_LONGITUDE = 39.8262

    /**
     * Calculates the bearing (azimuth) from the user's location to the Kaaba in Mecca.
     * Returns the angle in degrees relative to True North (0-360).
     */
    fun calculateQiblaBearing(userLat: Double, userLng: Double): Double {
        val userLatRad = Math.toRadians(userLat)
        val userLngRad = Math.toRadians(userLng)
        val meccaLatRad = Math.toRadians(MECCA_LATITUDE)
        val meccaLngRad = Math.toRadians(MECCA_LONGITUDE)

        val deltaLng = meccaLngRad - userLngRad

        val y = sin(deltaLng) * cos(meccaLatRad)
        val x = cos(userLatRad) * sin(meccaLatRad) -
                sin(userLatRad) * cos(meccaLatRad) * cos(deltaLng)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        
        return bearing
    }

    /**
     * Calculates the distance from the user's location to the Kaaba in Mecca in kilometers.
     */
    fun calculateDistanceToMecca(userLat: Double, userLng: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(MECCA_LATITUDE - userLat)
        val dLng = Math.toRadians(MECCA_LONGITUDE - userLng)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(MECCA_LATITUDE)) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return r * c
    }
}
