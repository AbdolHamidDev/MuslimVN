package com.example.muslimvn.domain.models

import java.time.LocalDate

/**
 * A key Islamic event anchored to a concrete Gregorian [localDate] within a Hijri
 * year, together with the number of days remaining until it occurs
 * (`countdownDays` negative = already passed).
 */
data class HijriUpcomingEvent(
    val event: IslamicEvent,
    val localDate: LocalDate,
    val countdownDays: Long
)