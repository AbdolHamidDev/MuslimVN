package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.PrayerTimes
import java.util.Date

interface PrayerRepository {
    fun getPrayerTimes(latitude: Double, longitude: Double, date: Date): PrayerTimes
}
