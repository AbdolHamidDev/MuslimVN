package com.example.muslimvn.data.repository

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.DetailedPrayerState
import com.example.muslimvn.domain.models.PrayerCalculationProfile
import com.example.muslimvn.domain.models.PrayerLocation
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerTimeWindow
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.models.RestrictedPeriod
import com.example.muslimvn.domain.models.RestrictedPeriodType
import com.example.muslimvn.domain.repository.PrayerRepository
import com.example.muslimvn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : PrayerRepository {

    private val vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    override fun getPrayerTimes(latitude: Double, longitude: Double, date: Date): PrayerTimes {
        val coordinates = Coordinates(latitude, longitude)

        // Load calculation preferences
        val methodStr = runBlocking { settingsRepository.getCalculationMethod().first() }
        val asrMethod = runBlocking { settingsRepository.getAsrMethod().first() }
        val adjustments = runBlocking { settingsRepository.getPrayerAdjustments().first() }

        val params = getCalculationParameters(methodStr, asrMethod)

        // Calculate today's, tomorrow's, and yesterday's prayer times
        val todayCalendar = Calendar.getInstance(vnTimeZone).apply { time = date }
        val todayComponents = DateComponents.from(todayCalendar.time)
        val todayAdhan = AdhanPrayerTimes(coordinates, todayComponents, params)

        // Also calculate Hanafi parameters for today to determine standard Asr preferred end boundary
        val hanafiParams = getCalculationParameters(methodStr, AsrMethod.HANAFI)
        val todayHanafiAdhan = AdhanPrayerTimes(coordinates, todayComponents, hanafiParams)

        val tomorrowCalendar = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowComponents = DateComponents.from(tomorrowCalendar.time)
        val tomorrowAdhan = AdhanPrayerTimes(coordinates, tomorrowComponents, params)

        val yesterdayCalendar = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayComponents = DateComponents.from(yesterdayCalendar.time)
        val yesterdayAdhan = AdhanPrayerTimes(coordinates, yesterdayComponents, params)

        // Raw dates from Adhan
        val rawFajr = todayAdhan.fajr ?: date
        val rawSunrise = todayAdhan.sunrise ?: date
        val rawDhuhr = todayAdhan.dhuhr ?: date
        val rawAsr = todayAdhan.asr ?: date
        val rawHanafiAsr = todayHanafiAdhan.asr ?: rawAsr
        val rawMaghrib = todayAdhan.maghrib ?: date
        val rawIsha = todayAdhan.isha ?: date

        val rawTomorrowFajr = tomorrowAdhan.fajr ?: Date(date.time + TimeUnit.DAYS.toMillis(1))
        val rawYesterdayIsha = yesterdayAdhan.isha ?: Date(rawIsha.time - TimeUnit.DAYS.toMillis(1))

        // Apply manual minute adjustments
        val fajr = applyAdjustment(rawFajr, adjustments.fajr)
        val sunrise = rawSunrise // Sunrise is astronomical event
        val dhuhr = applyAdjustment(rawDhuhr, adjustments.dhuhr)
        val solarTransit = rawDhuhr
        val asr = applyAdjustment(rawAsr, adjustments.asr)
        val maghrib = applyAdjustment(rawMaghrib, adjustments.maghrib)
        val sunset = maghrib
        val isha = applyAdjustment(rawIsha, adjustments.isha)

        val tomorrowFajr = applyAdjustment(rawTomorrowFajr, adjustments.fajr)
        val yesterdayIsha = applyAdjustment(rawYesterdayIsha, adjustments.isha)

        // Asr Preferred End Boundary (late Asr / yellowing sun phase)
        val rawAsrPreferredEnd = if (asrMethod == AsrMethod.STANDARD && rawHanafiAsr.after(rawAsr) && rawHanafiAsr.before(rawMaghrib)) {
            rawHanafiAsr
        } else {
            Date(sunset.time - 45 * 60_000L)
        }
        val asrPreferredEnd = applyAdjustment(rawAsrPreferredEnd, adjustments.asr)

        // Islamic Midnight = Sunset + (Next Fajr - Sunset) / 2
        val islamicMidnight = Date(sunset.time + (tomorrowFajr.time - sunset.time) / 2)

        // Construct explicit Prayer Time Windows
        val fajrWindow = PrayerTimeWindow(
            name = PrayerName.FAJR,
            startTime = fajr,
            finalEndTime = sunrise
        )
        val dhuhrWindow = PrayerTimeWindow(
            name = PrayerName.DHUHR,
            startTime = dhuhr,
            finalEndTime = asr
        )
        val asrWindow = PrayerTimeWindow(
            name = PrayerName.ASR,
            startTime = asr,
            preferredEndTime = asrPreferredEnd,
            finalEndTime = sunset
        )
        val maghribWindow = PrayerTimeWindow(
            name = PrayerName.MAGHRIB,
            startTime = maghrib,
            finalEndTime = isha
        )
        val ishaWindow = PrayerTimeWindow(
            name = PrayerName.ISHA,
            startTime = isha,
            preferredEndTime = islamicMidnight,
            finalEndTime = tomorrowFajr
        )

        // Restricted periods (pure astronomical events with separate operational safety approximations)
        val restrictedPeriods = listOf(
            RestrictedPeriod(
                type = RestrictedPeriodType.SUNRISE,
                astronomicalStart = sunrise,
                astronomicalEnd = sunrise,
                operationalSafetyApproximationMinutes = 15
            ),
            RestrictedPeriod(
                type = RestrictedPeriodType.ZAWAL,
                astronomicalStart = solarTransit,
                astronomicalEnd = dhuhr,
                operationalSafetyApproximationMinutes = 0
            ),
            RestrictedPeriod(
                type = RestrictedPeriodType.SUNSET,
                astronomicalStart = sunset,
                astronomicalEnd = sunset,
                operationalSafetyApproximationMinutes = 15
            )
        )

        // Determine Detailed Prayer State, Current Prayer, Next Prayer, and Countdown
        val now = date
        val (detailedState, currentPrayerName, isCurrentActive, nextPrayerName, nextPrayerTime, previousPrayerTime) =
            evaluatePrayerState(
                now = now,
                fajr = fajr,
                sunrise = sunrise,
                dhuhr = dhuhr,
                asr = asr,
                asrPreferredEnd = asrPreferredEnd,
                maghrib = maghrib,
                isha = isha,
                islamicMidnight = islamicMidnight,
                tomorrowFajr = tomorrowFajr,
                yesterdayIsha = yesterdayIsha
            )

        val countdownStr = formatCountdown(now, nextPrayerTime)

        val profile = PrayerCalculationProfile(
            id = if (methodStr == "MUSLIMVN_DEFAULT") "MUSLIMVN_DEFAULT" else methodStr,
            displayName = if (methodStr == "MUSLIMVN_DEFAULT") "MuslimVN mặc định" else methodStr,
            timezone = ZoneId.of("Asia/Ho_Chi_Minh"),
            fajrAngle = params.fajrAngle,
            ishaAngle = params.ishaAngle,
            asrMethod = asrMethod
        )

        val localDate = LocalDate.of(
            todayCalendar.get(Calendar.YEAR),
            todayCalendar.get(Calendar.MONTH) + 1,
            todayCalendar.get(Calendar.DAY_OF_MONTH)
        )

        return PrayerTimes(
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            solarTransit = solarTransit,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            nextPrayerName = nextPrayerName,
            nextPrayerTime = nextPrayerTime,
            nextPrayerCountdown = countdownStr,
            previousPrayerTime = previousPrayerTime,
            currentPrayerName = currentPrayerName,
            isCurrentPrayerActive = isCurrentActive,
            detailedState = detailedState,
            date = localDate,
            location = PrayerLocation(latitude, longitude),
            timezone = ZoneId.of("Asia/Ho_Chi_Minh"),
            calculationProfile = profile,
            adjustments = adjustments,
            fajrWindow = fajrWindow,
            dhuhrWindow = dhuhrWindow,
            asrWindow = asrWindow,
            maghribWindow = maghribWindow,
            ishaWindow = ishaWindow,
            sunset = sunset,
            islamicMidnight = islamicMidnight,
            restrictedPeriods = restrictedPeriods
        )
    }

    private fun getCalculationParameters(methodStr: String, asrMethod: AsrMethod): CalculationParameters {
        val params = when (methodStr) {
            "MUSLIMVN_DEFAULT" -> CalculationParameters(18.0, 18.0)
            else -> try {
                CalculationMethod.valueOf(methodStr).parameters
            } catch (e: Exception) {
                CalculationParameters(18.0, 18.0)
            }
        }
        params.madhab = if (asrMethod == AsrMethod.HANAFI) Madhab.HANAFI else Madhab.SHAFI
        return params
    }

    private fun applyAdjustment(baseTime: Date, adjustmentMinutes: Int): Date {
        if (adjustmentMinutes == 0) return baseTime
        return Date(baseTime.time + adjustmentMinutes * 60_000L)
    }

    private fun evaluatePrayerState(
        now: Date,
        fajr: Date,
        sunrise: Date,
        dhuhr: Date,
        asr: Date,
        asrPreferredEnd: Date,
        maghrib: Date,
        isha: Date,
        islamicMidnight: Date,
        tomorrowFajr: Date,
        yesterdayIsha: Date
    ): PrayerStateEvaluation {
        return when {
            now.before(fajr) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.BEFORE_FAJR,
                currentPrayerName = null,
                isCurrentPrayerActive = false,
                nextPrayerName = PrayerName.FAJR,
                nextPrayerTime = fajr,
                previousPrayerTime = yesterdayIsha
            )
            !now.before(fajr) && now.before(sunrise) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.FAJR,
                currentPrayerName = PrayerName.FAJR,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.DHUHR,
                nextPrayerTime = dhuhr,
                previousPrayerTime = fajr
            )
            !now.before(sunrise) && now.before(Date(sunrise.time + 15 * 60_000L)) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.SUNRISE_RESTRICTED,
                currentPrayerName = null,
                isCurrentPrayerActive = false,
                nextPrayerName = PrayerName.DHUHR,
                nextPrayerTime = dhuhr,
                previousPrayerTime = sunrise
            )
            !now.before(Date(sunrise.time + 15 * 60_000L)) && now.before(dhuhr) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.BETWEEN_PRAYERS,
                currentPrayerName = null,
                isCurrentPrayerActive = false,
                nextPrayerName = PrayerName.DHUHR,
                nextPrayerTime = dhuhr,
                previousPrayerTime = sunrise
            )
            !now.before(dhuhr) && now.before(asr) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.DHUHR,
                currentPrayerName = PrayerName.DHUHR,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.ASR,
                nextPrayerTime = asr,
                previousPrayerTime = dhuhr
            )
            !now.before(asr) && now.before(asrPreferredEnd) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.ASR,
                currentPrayerName = PrayerName.ASR,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.MAGHRIB,
                nextPrayerTime = maghrib,
                previousPrayerTime = asr
            )
            !now.before(asrPreferredEnd) && now.before(maghrib) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.LATE_ASR,
                currentPrayerName = PrayerName.ASR,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.MAGHRIB,
                nextPrayerTime = maghrib,
                previousPrayerTime = asr
            )
            !now.before(maghrib) && now.before(isha) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.MAGHRIB,
                currentPrayerName = PrayerName.MAGHRIB,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.ISHA,
                nextPrayerTime = isha,
                previousPrayerTime = maghrib
            )
            !now.before(isha) && now.before(islamicMidnight) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.ISHA,
                currentPrayerName = PrayerName.ISHA,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.FAJR,
                nextPrayerTime = tomorrowFajr,
                previousPrayerTime = isha
            )
            !now.before(islamicMidnight) && now.before(tomorrowFajr) -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.AFTER_ISHA,
                currentPrayerName = PrayerName.ISHA,
                isCurrentPrayerActive = true,
                nextPrayerName = PrayerName.FAJR,
                nextPrayerTime = tomorrowFajr,
                previousPrayerTime = isha
            )
            else -> PrayerStateEvaluation(
                detailedState = DetailedPrayerState.BEFORE_NEXT_FAJR,
                currentPrayerName = null,
                isCurrentPrayerActive = false,
                nextPrayerName = PrayerName.FAJR,
                nextPrayerTime = tomorrowFajr,
                previousPrayerTime = isha
            )
        }
    }

    private fun formatCountdown(now: Date, targetTime: Date): String {
        var diff = targetTime.time - now.time
        if (diff < 0) {
            diff += TimeUnit.DAYS.toMillis(1)
        }
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes)
    }

    private data class PrayerStateEvaluation(
        val detailedState: DetailedPrayerState,
        val currentPrayerName: String?,
        val isCurrentPrayerActive: Boolean,
        val nextPrayerName: String,
        val nextPrayerTime: Date,
        val previousPrayerTime: Date
    )
}
