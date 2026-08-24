package com.example.muslimvn.domain.util

import java.util.Locale

/**
 * Hijri month names (Arabic transliteration), indexed by Hijri month number (1..12).
 */
object HijriMonthNames {

    val ARABIC: List<String> = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-awwal",
        "Rabi' al-thani",
        "Jumada al-awwal",
        "Jumada al-thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    fun monthName(monthNumber: Int): String =
        ARABIC.getOrElse(monthNumber - 1) { "Month ${monthNumber.coerceIn(1, 12)}" }

    /** Case-insensitive lookup used to resolve names returned by the Aladhan API. */
    fun monthNumberForAlias(alias: String): Int? {
        val normalized = alias.trim().lowercase(Locale.ROOT)
        return ARABIC.indexOfFirst { it.lowercase(Locale.ROOT) == normalized }
            .takeIf { it >= 0 }
            ?.plus(1)
    }
}