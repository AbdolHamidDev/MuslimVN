package com.example.muslimvn.core.utils

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.*

object PrayerUtils {
    fun getPrayerTimes(latitude: Double, longitude: Double): PrayerTimes {
        val coordinates = Coordinates(latitude, longitude)
        val date = DateComponents.from(Date())
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        return PrayerTimes(coordinates, date, params)
    }
}
