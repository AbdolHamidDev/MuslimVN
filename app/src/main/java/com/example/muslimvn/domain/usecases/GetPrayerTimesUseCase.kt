package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.repository.LocationRepository
import com.example.muslimvn.domain.repository.PrayerRepository
import java.util.Date
import javax.inject.Inject

class GetPrayerTimesUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val prayerRepository: PrayerRepository
) {
    suspend operator fun invoke(
        date: Date = Date(),
        lat: Double? = null,
        lng: Double? = null
    ): PrayerTimes {
        val actualLat: Double
        val actualLng: Double

        if (lat != null && lng != null) {
            actualLat = lat
            actualLng = lng
        } else {
            val location = locationRepository.getCurrentLocation()
            actualLat = location?.latitude ?: 10.7005 // Chau Doc fallback
            actualLng = location?.longitude ?: 105.1147
        }
        
        return prayerRepository.getPrayerTimes(actualLat, actualLng, date)
    }
}
