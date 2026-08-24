package com.example.muslimvn.domain.models

import androidx.annotation.StringRes

/**
 * A fixed Islamic event anchored on a repeating Hijri (month, day),
 * e.g. 1 Shawwal (Eid al-Fitr), 9 Dhu al-Hijjah (Arafah), 10 Dhu al-Hijjah (Eid al-Adha).
 * Localized Vietnamese label lives in `strings.xml` and is referenced by [nameResId].
 */
data class IslamicEvent(
    val id: String,
    @StringRes val nameResId: Int,
    val nameArabic: String,
    val hijriMonth: Int, // 1..12
    val hijriDay: Int
)