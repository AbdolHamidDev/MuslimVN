package com.example.muslimvn.data.repository

import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.DetailedPrayerState
import com.example.muslimvn.domain.models.PrayerAdjustments
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.RestrictedPeriodType
import com.example.muslimvn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class PrayerRepositoryImplTest {

    private lateinit var repository: PrayerRepositoryImpl
    private val vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    // Fake SettingsRepository
    private class FakeSettingsRepository(
        var calcMethod: String = "MUSLIMVN_DEFAULT",
        var asrMethod: AsrMethod = AsrMethod.STANDARD,
        var adjustments: PrayerAdjustments = PrayerAdjustments()
    ) : SettingsRepository {
        override fun getPrayerReminders(): Flow<Map<String, PrayerReminder>> = flowOf(emptyMap())
        override suspend fun updateReminder(reminder: PrayerReminder) {}
        override fun getAppTheme(): Flow<AppTheme> = flowOf(AppTheme.FOLLOW_SYSTEM)
        override suspend fun updateAppTheme(theme: AppTheme) {}
        override fun getCalculationMethod(): Flow<String> = flowOf(calcMethod)
        override suspend fun updateCalculationMethod(method: String) { calcMethod = method }
        override fun getAsrMethod(): Flow<AsrMethod> = flowOf(asrMethod)
        override suspend fun updateAsrMethod(method: AsrMethod) { asrMethod = method }
        override fun getPrayerAdjustments(): Flow<PrayerAdjustments> = flowOf(adjustments)
        override suspend fun updatePrayerAdjustments(adjustments: PrayerAdjustments) { this.adjustments = adjustments }
    }

    private lateinit var fakeSettings: FakeSettingsRepository

    @Before
    fun setUp() {
        fakeSettings = FakeSettingsRepository()
        repository = PrayerRepositoryImpl(fakeSettings)
    }

    private fun createVnDate(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Date {
        val cal = Calendar.getInstance(vnTimeZone)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    @Test
    fun testPrayerTimesOrder_HoChiMinh() {
        val lat = 10.8231
        val lng = 106.6297
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        val times = repository.getPrayerTimes(lat, lng, testDate)

        assertTrue("Fajr should be before Sunrise", times.fajr.before(times.sunrise))
        assertTrue("Sunrise should be before Dhuhr", times.sunrise.before(times.dhuhr))
        assertTrue("Dhuhr should be before Asr", times.dhuhr.before(times.asr))
        assertTrue("Asr should be before Maghrib", times.asr.before(times.maghrib))
        assertTrue("Maghrib should be before Isha", times.maghrib.before(times.isha))
    }

    @Test
    fun testIslamicMidnightCalculation() {
        val lat = 10.8231
        val lng = 106.6297
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        val times = repository.getPrayerTimes(lat, lng, testDate)

        val midnight = times.islamicMidnight
        assertNotNull("Islamic Midnight must not be null", midnight)
        if (midnight != null) {
            assertTrue("Islamic Midnight must be after Sunset", midnight.after(times.sunset))
            assertTrue("Islamic Midnight must be after Isha", midnight.after(times.isha))
        }
    }

    @Test
    fun testHanafiAsrIsLaterThanStandardAsr() {
        val lat = 21.0285 // Hanoi
        val lng = 105.8542
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        fakeSettings.asrMethod = AsrMethod.STANDARD
        val standardTimes = repository.getPrayerTimes(lat, lng, testDate)

        fakeSettings.asrMethod = AsrMethod.HANAFI
        val hanafiTimes = repository.getPrayerTimes(lat, lng, testDate)

        assertTrue("Hanafi Asr should be strictly later than Standard Asr", hanafiTimes.asr.after(standardTimes.asr))
    }

    @Test
    fun testManualAdjustments() {
        val lat = 10.8231
        val lng = 106.6297
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        fakeSettings.adjustments = PrayerAdjustments()
        val baseTimes = repository.getPrayerTimes(lat, lng, testDate)

        fakeSettings.adjustments = PrayerAdjustments(fajr = 5, dhuhr = -2)
        val adjustedTimes = repository.getPrayerTimes(lat, lng, testDate)

        val fajrDiffMinutes = Math.round((adjustedTimes.fajr.time - baseTimes.fajr.time) / 60_000.0)
        val dhuhrDiffMinutes = Math.round((adjustedTimes.dhuhr.time - baseTimes.dhuhr.time) / 60_000.0)

        assertEquals(5L, fajrDiffMinutes)
        assertEquals(-2L, dhuhrDiffMinutes)
    }

    @Test
    fun testAsrPreferredEndTime() {
        val lat = 10.8231
        val lng = 106.6297
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        val times = repository.getPrayerTimes(lat, lng, testDate)
        val asrWindow = times.asrWindow

        assertNotNull("Asr window must exist", asrWindow)
        val preferredEnd = asrWindow?.preferredEndTime
        assertNotNull("Asr preferred end time must exist", preferredEnd)

        if (preferredEnd != null && asrWindow != null) {
            assertTrue("Asr preferred end time must be strictly after Asr start", preferredEnd.after(asrWindow.startTime))
            assertTrue("Asr preferred end time must be strictly before Sunset", preferredEnd.before(asrWindow.finalEndTime))
        }
    }

    @Test
    fun testCurrentTimeBetweenAsrPreferredEndAndSunset() {
        val lat = 10.8231
        val lng = 106.6297
        val baseNoon = createVnDate(2026, 6, 15, 12, 0)
        val times = repository.getPrayerTimes(lat, lng, baseNoon)

        val preferredEnd = times.asrWindow?.preferredEndTime ?: return
        val lateAsrTime = Date(preferredEnd.time + 5 * 60_000L) // 5 minutes after preferred end

        val lateAsrState = repository.getPrayerTimes(lat, lng, lateAsrTime)

        assertEquals(DetailedPrayerState.LATE_ASR, lateAsrState.detailedState)
        assertEquals(PrayerName.ASR, lateAsrState.currentPrayerName)
        assertTrue("Asr prayer must remain ACTIVE during late Asr phase before sunset", lateAsrState.isCurrentPrayerActive)
    }

    @Test
    fun testNoArtificialZawal10MinutesPeriod() {
        val lat = 10.8231
        val lng = 106.6297
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        val times = repository.getPrayerTimes(lat, lng, testDate)
        val zawalPeriod = times.restrictedPeriods.find { it.type == RestrictedPeriodType.ZAWAL }

        assertNotNull("Zawal restricted period must exist", zawalPeriod)
        assertEquals(
            "Zawal astronomical start must equal solar transit time",
            times.solarTransit.time,
            zawalPeriod?.astronomicalStart?.time
        )
    }

    @Test
    fun testOverlappingAsrAndSunsetSafetyBuffer() {
        val lat = 10.8231
        val lng = 106.6297
        val baseNoon = createVnDate(2026, 6, 15, 12, 0)
        val times = repository.getPrayerTimes(lat, lng, baseNoon)

        // 10 minutes before sunset
        val nearSunsetTime = Date(times.sunset.time - 10 * 60_000L)
        val state = repository.getPrayerTimes(lat, lng, nearSunsetTime)

        assertEquals(PrayerName.ASR, state.currentPrayerName)
        assertTrue("Asr must remain ACTIVE during operational sunset safety buffer before sunset", state.isCurrentPrayerActive)
    }

    @Test
    fun testIshaPreferredAndFinalEndTimes() {
        val lat = 10.8231
        val lng = 106.6297
        val testDate = createVnDate(2026, 6, 15, 12, 0)

        val times = repository.getPrayerTimes(lat, lng, testDate)
        val ishaWindow = times.ishaWindow

        assertNotNull("Isha window must exist", ishaWindow)
        assertEquals("Isha preferred end time must equal Islamic Midnight", times.islamicMidnight, ishaWindow?.preferredEndTime)
        val midnight = times.islamicMidnight
        if (midnight != null && ishaWindow != null) {
            assertTrue("Isha final end time must be after Islamic Midnight", ishaWindow.finalEndTime.after(midnight))
        }
    }

    @Test
    fun testPrayerWindowsAndStates() {
        val lat = 16.0544 // Da Nang
        val lng = 108.2022
        val baseNoon = createVnDate(2026, 6, 15, 12, 0)
        val times = repository.getPrayerTimes(lat, lng, baseNoon)

        // Test before Fajr
        val beforeFajrTime = Date(times.fajr.time - 30 * 60_000L)
        val beforeFajrState = repository.getPrayerTimes(lat, lng, beforeFajrTime)
        assertEquals(DetailedPrayerState.BEFORE_FAJR, beforeFajrState.detailedState)
        assertEquals(PrayerName.FAJR, beforeFajrState.nextPrayerName)

        // Test during Dhuhr
        val duringDhuhrTime = Date(times.dhuhr.time + 30 * 60_000L)
        val duringDhuhrState = repository.getPrayerTimes(lat, lng, duringDhuhrTime)
        assertEquals(DetailedPrayerState.DHUHR, duringDhuhrState.detailedState)
        assertEquals(PrayerName.DHUHR, duringDhuhrState.currentPrayerName)
        assertTrue(duringDhuhrState.isCurrentPrayerActive)
        assertEquals(PrayerName.ASR, duringDhuhrState.nextPrayerName)

        // Test during Isha
        val duringIshaTime = Date(times.isha.time + 30 * 60_000L)
        val duringIshaState = repository.getPrayerTimes(lat, lng, duringIshaTime)
        assertEquals(DetailedPrayerState.ISHA, duringIshaState.detailedState)
        assertEquals(PrayerName.ISHA, duringIshaState.currentPrayerName)
        assertTrue(duringIshaState.isCurrentPrayerActive)
        assertEquals(PrayerName.FAJR, duringIshaState.nextPrayerName)
    }

    @Test
    fun testDateRolloverAtLateNight() {
        val lat = 10.8231
        val lng = 106.6297

        // Late night 23:45
        val lateNightTime = createVnDate(2026, 6, 15, 23, 45)
        val lateState = repository.getPrayerTimes(lat, lng, lateNightTime)

        assertEquals(PrayerName.FAJR, lateState.nextPrayerName)
        assertTrue("Next prayer time should be after 23:45", lateState.nextPrayerTime.after(lateNightTime))

        // Early morning 02:30 (before Fajr)
        val earlyMorningTime = createVnDate(2026, 6, 16, 2, 30)
        val earlyState = repository.getPrayerTimes(lat, lng, earlyMorningTime)

        assertEquals(PrayerName.FAJR, earlyState.nextPrayerName)
        assertTrue("Next prayer time should be after 02:30", earlyState.nextPrayerTime.after(earlyMorningTime))
    }

    @Test
    fun testMultipleVietnameseLocations() {
        val testDate = createVnDate(2026, 6, 15, 12, 0)
        val cities = listOf(
            "Hanoi" to (21.0285 to 105.8542),
            "Da Nang" to (16.0544 to 108.2022),
            "Ho Chi Minh City" to (10.8231 to 106.6297),
            "Chau Doc" to (10.7005 to 105.1147),
            "Sapa" to (22.3364 to 103.8438)
        )

        for ((cityName, coords) in cities) {
            val times = repository.getPrayerTimes(coords.first, coords.second, testDate)
            assertNotNull("Prayer times for $cityName must be non-null", times)
            assertTrue("$cityName Fajr < Sunrise", times.fajr.before(times.sunrise))
            assertTrue("$cityName Sunrise < Dhuhr", times.sunrise.before(times.dhuhr))
            assertTrue("$cityName Dhuhr < Asr", times.dhuhr.before(times.asr))
            assertTrue("$cityName Asr < Maghrib", times.asr.before(times.maghrib))
            assertTrue("$cityName Maghrib < Isha", times.maghrib.before(times.isha))
        }
    }
}
