package com.example.muslimvn.domain.models

import java.util.Date

data class PrayerTimes(
    val fajr: Date,
    val sunrise: Date,
    val dhuhr: Date,
    val asr: Date,
    val maghrib: Date,
    val isha: Date,
    val nextPrayerName: String,
    val nextPrayerTime: Date,
    val nextPrayerCountdown: String
)

enum class PrayerType(val displayName: String) {
    FAJR("Fajr"),
    SUNRISE("Sunrise"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
}
