package com.example.muslimvn.domain.util

import com.example.muslimvn.domain.models.HijriUpcomingEvent
import com.example.muslimvn.domain.models.IslamicEvent
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * Pure date-math helpers on top of `java.time.chrono.HijrahChronology`.
 * Used both for the offline fallback and for computing event countdowns.
 */
object HijriCalendarUtils {

    /** Converts a Hijri (year, month, day) to the equivalent [LocalDate], or null if invalid. */
    fun hijriToGregorian(hijriYear: Int, hijriMonth: Int, hijriDay: Int): LocalDate? =
        runCatching {
            val hijriDate = HijrahDate.of(hijriYear, hijriMonth, hijriDay)
            LocalDate.ofEpochDay(hijriDate.toEpochDay())
        }.getOrNull()

    /** Shifts a Hijri date by [offsetDays] (may cross month/year boundaries). */
    fun shiftHijriDate(hijriYear: Int, hijriMonth: Int, hijriDay: Int, offsetDays: Int): HijrahDate? =
        if (offsetDays == 0) {
            runCatching { HijrahDate.of(hijriYear, hijriMonth, hijriDay) }.getOrNull()
        } else {
            runCatching {
                HijrahDate.of(hijriYear, hijriMonth, hijriDay)
                    .plus(offsetDays.toLong(), ChronoUnit.DAYS)
            }.getOrNull()
        }

    /**
     * Returns the (offset-shifted) Hijri day-of-month for a Gregorian date, or null when the
     * date cannot be converted. Used to render leading/trailing cells of the month grid.
     */
    fun hijriDayNumberFor(gregorianDate: LocalDate, offsetDays: Int): Int? =
        runCatching {
            HijrahDate.from(gregorianDate)
                .plus(offsetDays.toLong(), ChronoUnit.DAYS)
                .get(ChronoField.DAY_OF_MONTH)
        }.getOrNull()

    /**
     * Returns the full (offset-shifted) [HijrahDate] for a Gregorian date, or null when the
     * conversion fails. Shared by the Home header and the calendar grid so both screens
     * always agree on "hôm nay là ngày Hijri nào".
     */
    fun hijriDateFor(gregorianDate: LocalDate, offsetDays: Int): HijrahDate? =
        runCatching {
            HijrahDate.from(gregorianDate).plus(offsetDays.toLong(), ChronoUnit.DAYS)
        }.getOrNull()

    /**
     * All catalog events anchored to [hijriYear], with concrete Gregorian dates and the
     * days remaining until each one (negative when already passed), sorted chronologically.
     */
    fun eventsForHijriYear(hijriYear: Int, today: LocalDate = LocalDate.now()): List<HijriUpcomingEvent> =
        IslamicEvents.ALL.mapNotNull { event ->
            val localDate = hijriToGregorian(hijriYear, event.hijriMonth, event.hijriDay)
                ?: return@mapNotNull null
            HijriUpcomingEvent(
                event = event,
                localDate = localDate,
                countdownDays = ChronoUnit.DAYS.between(today, localDate)
            )
        }.sortedWith(compareBy({ it.localDate }, { it.event.hijriMonth * 100 + it.event.hijriDay }))
}