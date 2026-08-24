package com.example.muslimvn.data.repository

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.repository.PrayerRepository
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepositoryImpl @Inject constructor() : PrayerRepository {

    override fun getPrayerTimes(latitude: Double, longitude: Double, date: Date): PrayerTimes {
        val coordinates = Coordinates(latitude, longitude)
        val calendar = Calendar.getInstance().apply { time = date }
        val dateComponents = DateComponents.from(date)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters

        val adhanPrayerTimes = AdhanPrayerTimes(coordinates, dateComponents, params)

        val nextPrayer = adhanPrayerTimes.nextPrayer()
        val nextPrayerTime = adhanPrayerTimes.timeForPrayer(nextPrayer) ?: adhanPrayerTimes.fajr
        
        return PrayerTimes(
            fajr = adhanPrayerTimes.fajr,
            sunrise = adhanPrayerTimes.sunrise,
            dhuhr = adhanPrayerTimes.dhuhr,
            asr = adhanPrayerTimes.asr,
            maghrib = adhanPrayerTimes.maghrib,
            isha = adhanPrayerTimes.isha,
            nextPrayerName = nextPrayer.name,
            nextPrayerTime = nextPrayerTime,
            nextPrayerCountdown = formatCountdown(nextPrayerTime)
        )
    }

    private fun formatCountdown(targetTime: Date): String {
        val now = Date()
        var diff = targetTime.time - now.time
        
        // If target time is before now, it might be Fajr of next day
        if (diff < 0) {
            diff += TimeUnit.DAYS.toMillis(1)
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return String.format("%02dh %02dm", hours, minutes)
    }
}
