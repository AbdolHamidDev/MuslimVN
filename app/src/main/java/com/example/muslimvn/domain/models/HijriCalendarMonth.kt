package com.example.muslimvn.domain.models

/**
 * The full month model rendered by [HijriCalendarScreen].
 *
 * `hijriMonthNumber` / `hijriYear` describe the *dominant* Hijri month shown in the
 * currently displayed Gregorian month (a Gregorian month usually spans two Hijri months),
 * and are used for the header, e.g. "Ramadan 1447 AH".
 *
 * `isUsingFallback = true` means no cached data existed and the app rendered from the
 * local `HijrahChronology` calculation instead of the Aladhan API / Room cache.
 */
data class HijriCalendarMonth(
    val gregorianMonth: Int, // 1..12
    val gregorianYear: Int,
    val days: List<HijriDay>,
    val hijriMonthNumber: Int,
    val hijriYear: Int,
    val isUsingFallback: Boolean = true
)