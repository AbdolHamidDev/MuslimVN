package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.HijriCalendarMonth
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the Islamic (Hijri) calendar.
 *
 * Strategy (offline-first):
 * 1. Room cache is queried first and emitted immediately for instant rendering.
 * 2. A network refresh (Aladhan API) runs in the background when the cache for a
 *    Gregorian month/year is missing or stale.
 * 3. If the network is unavailable *and* no cache exists, a local calculation based on
 *    `java.time.chrono.HijrahChronology` is emitted so the app never crashes.
 */
interface HijriCalendarRepository {

    fun observeCalendarMonth(gregorianMonth: Int, gregorianYear: Int): Flow<HijriCalendarMonth>

    /** Fetches the month from Aladhan only when online and the cache is missing/stale. */
    suspend fun refreshCalendarMonth(gregorianMonth: Int, gregorianYear: Int)

    /** DataStore preference the user uses to fine-tune rendered Hijri dates (-2..+2 days). */
    fun observeDateOffset(): Flow<Int>

    suspend fun setDateOffset(offsetDays: Int)

    companion object {
        const val MIN_OFFSET_DAYS = -2
        const val MAX_OFFSET_DAYS = 2
        const val DEFAULT_OFFSET_DAYS = 0
        const val CACHE_STALE_AFTER_MILLIS = 7 * 24 * 60 * 60 * 1000L // 1 week
    }
}