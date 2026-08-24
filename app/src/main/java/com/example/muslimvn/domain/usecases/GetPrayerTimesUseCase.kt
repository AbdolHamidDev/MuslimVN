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
    suspend operator fun invoke(date: Date = Date()): PrayerTimes {
        val location = locationRepository.getCurrentLocation()
        val lat = location?.latitude ?: 10.7005 // Chau Doc fallback
        val lng = location?.longitude ?: 105.1147
        
        return prayerRepository.getPrayerTimes(lat, lng, date)
    }
}
