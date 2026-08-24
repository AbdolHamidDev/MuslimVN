package com.example.muslimvn.data.repository

import com.example.muslimvn.core.utils.NetworkMonitor
import com.example.muslimvn.data.local.dao.HijriCalendarDao
import com.example.muslimvn.data.local.entities.HijriDayEntity
import com.example.muslimvn.data.preferences.HijriPreferences
import com.example.muslimvn.data.remote.AladhanApiService
import com.example.muslimvn.data.remote.AladhanCalendarParser
import com.example.muslimvn.data.util.HijriLocalCalculator
import com.example.muslimvn.domain.models.HijriCalendarMonth
import com.example.muslimvn.domain.models.HijriDay
import com.example.muslimvn.domain.repository.HijriCalendarRepository
import com.example.muslimvn.domain.util.HijriCalendarUtils
import com.example.muslimvn.domain.util.IslamicEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoField
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [HijriCalendarRepository].
 *
 * Observing a month:
 * 1. emits what is cached in Room immediately (true single source of truth);
 * 2. if nothing is cached, emits the local `HijrahChronology` calculation so the UI
 *    renders instantly and *never crashes*;
 * 3. a background [refreshCalendarMonth] call fills/updates the Room cache from Aladhan,
 *    which re-triggers the Room flow and re-renders the month with real API data.
 */
@Singleton
class HijriCalendarRepositoryImpl @Inject constructor(
    private val dao: HijriCalendarDao,
    private val api: AladhanApiService,
    private val parser: AladhanCalendarParser,
    private val preferences: HijriPreferences,
    private val networkMonitor: NetworkMonitor
) : HijriCalendarRepository {

    override fun observeCalendarMonth(gregorianMonth: Int, gregorianYear: Int): Flow<HijriCalendarMonth> =
        combine(
            dao.observeGregorianMonth(gregorianMonth, gregorianYear),
            preferences.offsetDays
        ) { cachedEntities, offsetDays ->
            if (cachedEntities.isNotEmpty()) {
                buildMonth(cachedEntities, offsetDays, gregorianMonth, gregorianYear, isFallback = false)
            } else {
                val localEntities = HijriLocalCalculator.generateGregorianMonth(gregorianMonth, gregorianYear)
                buildMonth(localEntities, offsetDays, gregorianMonth, gregorianYear, isFallback = true)
            }
        }

    override suspend fun refreshCalendarMonth(gregorianMonth: Int, gregorianYear: Int) {
        if (!networkMonitor.isOnline()) return

        val lastCachedAt = dao.getLastCachedAt(gregorianMonth, gregorianYear)
        if (lastCachedAt != null &&
            System.currentTimeMillis() - lastCachedAt < HijriCalendarRepository.CACHE_STALE_AFTER_MILLIS
        ) {
            return // cache is fresh enough
        }

        // Never let network problems crash the calendar: failure => keep whatever is cached.
        runCatching {
            val body = api.getGregorianToHijriCalendar(gregorianMonth, gregorianYear)
            val entities = parser.parseMonthlyCalendar(body, gregorianMonth, gregorianYear)
            if (entities.isNotEmpty()) {
                dao.insertDays(entities)
            }
        }
    }

    override fun observeDateOffset(): Flow<Int> = preferences.offsetDays

    override suspend fun setDateOffset(offsetDays: Int) {
        preferences.setOffsetDays(offsetDays)
    }

    // ---- internal helpers ------------------------------------------------------------

    private fun buildMonth(
        entities: List<HijriDayEntity>,
        offsetDays: Int,
        gregorianMonth: Int,
        gregorianYear: Int,
        isFallback: Boolean
    ): HijriCalendarMonth {
        val days = entities.map { entity -> entity.toHijriDay(offsetDays) }
        val summary = dominantHijriMonth(days)
        return HijriCalendarMonth(
            gregorianMonth = gregorianMonth,
            gregorianYear = gregorianYear,
            days = days,
            hijriMonthNumber = summary.first,
            hijriYear = summary.second,
            isUsingFallback = isFallback
        )
    }

    /** Applies the user's Hijri date offset to a row and matches catalog events. */
    private fun HijriDayEntity.toHijriDay(offsetDays: Int): HijriDay {
        val gregorianDate = LocalDate.parse(gregorianDate)
        val adjusted = HijriCalendarUtils.shiftHijriDate(
            hijriYear, hijriMonth, hijriDay, offsetDays
        )
        return HijriDay(
            gregorianDate = gregorianDate,
            gregorianDay = gregorianDate.dayOfMonth,
            hijriDay = adjusted?.get(ChronoField.DAY_OF_MONTH) ?: hijriDay,
            hijriMonth = adjusted?.get(ChronoField.MONTH_OF_YEAR) ?: hijriMonth,
            hijriYear = adjusted?.get(ChronoField.YEAR) ?: hijriYear,
            weekday = gregorianDate.dayOfWeek.value,
            events = IslamicEvents.forHijriDay(
                adjusted?.get(ChronoField.MONTH_OF_YEAR) ?: hijriMonth,
                adjusted?.get(ChronoField.DAY_OF_MONTH) ?: hijriDay
            )
        )
    }

    /** Picks the Hijri month/year that covers most days of the displayed Gregorian month. */
    private fun dominantHijriMonth(days: List<HijriDay>): Pair<Int, Int> {
        if (days.isEmpty()) return Pair(1, LocalDate.now().year - 621)
        return days
            .groupBy { Pair(it.hijriMonth, it.hijriYear) }
            .maxWithOrNull(
                compareBy<Map.Entry<Pair<Int, Int>, List<HijriDay>>> { it.value.size }
                    .thenBy { it.key.first }
            )
            ?.key
            ?: Pair(1, LocalDate.now().year - 621)
    }
}