package com.example.muslimvn.domain.models

object PrayerName {
    const val FAJR = "Fajr"
    const val SUNRISE = "Sunrise"
    const val DHUHR = "Dhuhr"
    const val ASR = "Asr"
    const val MAGHRIB = "Maghrib"
    const val ISHA = "Isha"

    fun fromAdhanName(name: String): String {
        return when (name.uppercase()) {
            "FAJR" -> FAJR
            "SUNRISE" -> SUNRISE
            "DHUHR" -> DHUHR
            "ASR" -> ASR
            "MAGHRIB" -> MAGHRIB
            "ISHA" -> ISHA
            else -> name
        }
    }
}
