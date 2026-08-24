package com.example.muslimvn.domain.util

import com.example.muslimvn.R
import com.example.muslimvn.domain.models.IslamicEvent

/**
 * Hard-coded catalog of key Islamic events that recur every Hijri year.
 * Each event is localized in `strings.xml` through [IslamicEvent.nameResId].
 */
object IslamicEvents {

    val ALL: List<IslamicEvent> = listOf(
        IslamicEvent("new_year", R.string.event_islamic_new_year, "Ras as-Sanah al-Hijriyya", 1, 1),
        IslamicEvent("ashura", R.string.event_ashura, "Yawm al-Ashura", 1, 10),
        IslamicEvent("mawlid", R.string.event_mawlid, "Mawlid an-Nabi", 3, 12),
        IslamicEvent("isra_miraj", R.string.event_isra_miraj, "Al-Isra wal-Mi'raj", 7, 27),
        IslamicEvent("nisf_shaban", R.string.event_nisf_shaban, "Nisf Sha'ban", 8, 15),
        IslamicEvent("ramadan_start", R.string.event_ramadan_start, "Awwal Ramadan", 9, 1),
        IslamicEvent("laylatul_qadr", R.string.event_laylatul_qadr, "Laylat al-Qadr", 9, 27),
        IslamicEvent("eid_alfitr", R.string.event_eid_alfitr, "Eid al-Fitr", 10, 1),
        IslamicEvent("arafah", R.string.event_arafah, "Yawm al-Arafah", 12, 9),
        IslamicEvent("eid_aladha", R.string.event_eid_aladha, "Eid al-Adha", 12, 10)
    )

    /** Returns every catalog event falling on the given (offset-adjusted) Hijri date. */
    fun forHijriDay(hijriMonth: Int, hijriDay: Int): List<IslamicEvent> =
        ALL.filter { it.hijriMonth == hijriMonth && it.hijriDay == hijriDay }
}