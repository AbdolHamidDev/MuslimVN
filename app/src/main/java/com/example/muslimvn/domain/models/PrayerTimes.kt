package com.example.muslimvn.domain.models

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

enum class AsrMethod {
    STANDARD,
    HANAFI
}

enum class DetailedPrayerState {
    BEFORE_FAJR,
    FAJR,
    SUNRISE_RESTRICTED,
    BETWEEN_PRAYERS,
    DHUHR,
    ZAWAL_RESTRICTED,
    ASR,
    LATE_ASR,
    SUNSET_RESTRICTED,
    MAGHRIB,
    ISHA,
    AFTER_ISHA,
    BEFORE_NEXT_FAJR
}

data class PrayerCalculationProfile(
    val id: String,
    val displayName: String,
    val timezone: ZoneId,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val asrMethod: AsrMethod,
    val operationalSafetyBufferMinutes: Long = 0
) {
    companion object {
        val MUSLIMVN_DEFAULT = PrayerCalculationProfile(
            id = "MUSLIMVN_DEFAULT",
            displayName = "MuslimVN mặc định",
            timezone = ZoneId.of("Asia/Ho_Chi_Minh"),
            fajrAngle = 18.0,
            ishaAngle = 18.0,
            asrMethod = AsrMethod.STANDARD
        )
    }
}

data class PrayerLocation(
    val latitude: Double,
    val longitude: Double
)

data class PrayerTimeWindow(
    val name: String,
    val startTime: Date,
    val preferredEndTime: Date? = null,
    val finalEndTime: Date,
    val displayTime: Date = startTime
) {
    fun contains(time: Date): Boolean =
        !time.before(startTime) && time.before(finalEndTime)
}

enum class RestrictedPeriodType {
    SUNRISE,
    ZAWAL,
    SUNSET
}

data class RestrictedPeriod(
    val type: RestrictedPeriodType,
    val astronomicalStart: Date,
    val astronomicalEnd: Date,
    val operationalSafetyApproximationMinutes: Long = 0
) {
    fun contains(time: Date): Boolean =
        !time.before(astronomicalStart) && time.before(astronomicalEnd)
}

data class PrayerAdjustments(
    val fajr: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
)

data class PrayerTimes(
    val fajr: Date,
    val sunrise: Date,
    val dhuhr: Date,
    val solarTransit: Date = dhuhr,
    val asr: Date,
    val maghrib: Date,
    val isha: Date,
    val nextPrayerName: String,
    val nextPrayerTime: Date,
    val nextPrayerCountdown: String,
    val previousPrayerTime: Date = Date(),
    val currentPrayerName: String? = null,
    val isCurrentPrayerActive: Boolean = false,
    val detailedState: DetailedPrayerState = DetailedPrayerState.BETWEEN_PRAYERS,
    val date: LocalDate? = null,
    val location: PrayerLocation? = null,
    val timezone: ZoneId = PrayerCalculationProfile.MUSLIMVN_DEFAULT.timezone,
    val calculationProfile: PrayerCalculationProfile = PrayerCalculationProfile.MUSLIMVN_DEFAULT,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    val fajrWindow: PrayerTimeWindow? = null,
    val dhuhrWindow: PrayerTimeWindow? = null,
    val asrWindow: PrayerTimeWindow? = null,
    val maghribWindow: PrayerTimeWindow? = null,
    val ishaWindow: PrayerTimeWindow? = null,
    val sunset: Date = maghrib,
    val islamicMidnight: Date? = null,
    val restrictedPeriods: List<RestrictedPeriod> = emptyList()
)
