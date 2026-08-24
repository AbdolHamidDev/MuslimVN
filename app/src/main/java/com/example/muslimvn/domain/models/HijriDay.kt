package com.example.muslimvn.domain.models

import java.time.LocalDate

/**
 * One calendar day combining a fixed Gregorian date with its corresponding
 * Hijri date (already shifted by the user-selected Hijri date offset).
 */
data class HijriDay(
    val gregorianDate: LocalDate,
    val gregorianDay: Int,
    val hijriDay: Int,
    val hijriMonth: Int, // 1..12
    val hijriYear: Int,
    val weekday: Int, // ISO-8601 value Monday=1 .. Sunday=7
    val events: List<IslamicEvent> = emptyList()
) {
    val isToday: Boolean
        get() = gregorianDate == LocalDate.now()
}