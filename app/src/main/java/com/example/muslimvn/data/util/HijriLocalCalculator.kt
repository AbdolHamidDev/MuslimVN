package com.example.muslimvn.data.util

import com.example.muslimvn.data.local.entities.HijriDayEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * Offline fallback generator that builds the Hijri calendar entirely from
 * `java.time.chrono.HijrahChronology`. Because a dedicated Java implementation
 * performs the arithmetic, it always yields a valid month and can never crash.
 */
object HijriLocalCalculator {

    fun generateGregorianMonth(month: Int, year: Int): List<HijriDayEntity> {
        val yearMonth = YearMonth.of(year, month)
        return (1..yearMonth.lengthOfMonth()).map { day ->
            val gregorianDate = LocalDate.of(year, month, day)
            val hijriDate = HijrahDate.from(gregorianDate)
            HijriDayEntity(
                gregorianDate = gregorianDate.toString(),
                hijriDay = hijriDate.get(ChronoField.DAY_OF_MONTH),
                hijriMonth = hijriDate.get(ChronoField.MONTH_OF_YEAR),
                hijriYear = hijriDate.get(ChronoField.YEAR),
                dayOfWeek = gregorianDate.dayOfWeek.value,
                eventsJson = "[]",
                gregorianMonth = month,
                gregorianYear = year,
                cachedAtEpochMillis = 0L
            )
        }
    }
}