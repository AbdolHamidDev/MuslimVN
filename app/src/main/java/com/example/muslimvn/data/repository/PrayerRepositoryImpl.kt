package com.example.muslimvn.data.repository

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.repository.PrayerRepository
import com.example.muslimvn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : PrayerRepository {

    override fun getPrayerTimes(latitude: Double, longitude: Double, date: Date): PrayerTimes {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents.from(date)
        
        // Load saved calculation method
        val methodStr = runBlocking { settingsRepository.getCalculationMethod().first() }
        val method = try {
            CalculationMethod.valueOf(methodStr)
        } catch (e: Exception) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
        val params = method.parameters

        val adhanPrayerTimes = AdhanPrayerTimes(coordinates, dateComponents, params)

        var nextPrayer = adhanPrayerTimes.nextPrayer()
        var nextPrayerTime = adhanPrayerTimes.timeForPrayer(nextPrayer)
        var nextPrayerName = nextPrayer?.name ?: "NONE"

        // Nếu hôm nay đã hết các giờ cầu nguyện (sau Isha), mốc tiếp theo là Fajr ngày mai
        if ((nextPrayer == Prayer.NONE) || (nextPrayerTime == null)) {
            val tomorrow = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowTimes = AdhanPrayerTimes(coordinates, DateComponents.from(tomorrow.time), params)
            nextPrayer = Prayer.FAJR
            nextPrayerTime = tomorrowTimes.fajr
            nextPrayerName = nextPrayer.name
        }

        return PrayerTimes(
            fajr = adhanPrayerTimes.fajr ?: Date(),
            sunrise = adhanPrayerTimes.sunrise ?: Date(),
            dhuhr = adhanPrayerTimes.dhuhr ?: Date(),
            asr = adhanPrayerTimes.asr ?: Date(),
            maghrib = adhanPrayerTimes.maghrib ?: Date(),
            isha = adhanPrayerTimes.isha ?: Date(),
            nextPrayerName = PrayerName.fromAdhanName(nextPrayerName),
            nextPrayerTime = nextPrayerTime ?: Date(),
            nextPrayerCountdown = formatCountdown(nextPrayerTime ?: Date()),
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
        return String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes)
    }
}
