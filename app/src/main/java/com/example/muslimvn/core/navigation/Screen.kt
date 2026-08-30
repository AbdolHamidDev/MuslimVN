package com.example.muslimvn.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.muslimvn.R

sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Ibadah : Screen("ibadah", R.string.nav_ibadah, Icons.AutoMirrored.Filled.MenuBook)
    object Knowledge : Screen("knowledge", R.string.nav_knowledge, Icons.AutoMirrored.Filled.LibraryBooks)
    object Utilities : Screen("utilities", R.string.nav_utilities, Icons.Default.Widgets)
    object Local : Screen("local", R.string.nav_local, Icons.Default.LocationOn)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Tracker : Screen("tracker", R.string.nav_tracker, Icons.Default.Analytics)

    object Azkar : Screen("azkar", R.string.utility_azkar, Icons.Default.Menu)
    object Quran : Screen("quran", R.string.nav_quran, Icons.AutoMirrored.Filled.MenuBook)
    object Qibla : Screen("qibla", R.string.nav_qibla, Icons.Default.LocationOn)
    object HijriCalendar : Screen("hijri_calendar", R.string.nav_hijri_calendar, Icons.Default.Schedule)
    object NamesOfAllah : Screen("names_of_allah", R.string.nav_names_of_allah, Icons.AutoMirrored.Filled.MenuBook)
    object Zakat : Screen("zakat", R.string.utility_zakat, Icons.Default.Calculate)
    object QuranSettings : Screen("quran_settings?surahNumber={surahNumber}", R.string.nav_settings, Icons.Default.Settings) {
        fun createRoute(surahNumber: Int? = null) = 
            if (surahNumber != null) "quran_settings?surahNumber=$surahNumber" else "quran_settings"
    }
    object PrayerNotifications : Screen("prayer_notifications", R.string.nav_settings, Icons.Default.Notifications)
    object SurahDetail : Screen("surah_detail/{surahNumber}/{startAyah}", R.string.nav_surah_detail, Icons.Default.Menu) {
        fun createRoute(surahNumber: Int, startAyah: Int = 1) = "surah_detail/$surahNumber/$startAyah"
    }

    // ── Podcast học giả ─────────────────────────────────────────────────────────
    object PodcastHome : Screen("podcast_home", R.string.nav_podcast, Icons.Default.Podcasts)
    object ScholarDetail : Screen(
        route = "scholar_detail/{scholarId}",
        titleResId = R.string.nav_podcast,
        icon = Icons.Default.Podcasts
    ) {
        const val ARG_SCHOLAR_ID = "scholarId"
        fun createRoute(scholarId: String) = "scholar_detail/$scholarId"
    }
    object PodcastPlayer : Screen("podcast_player", R.string.nav_podcast, Icons.Default.Podcasts)

    object VietnamScholarDetail : Screen(
        route = "vietnam_scholar_detail/{scholarId}",
        titleResId = R.string.nav_podcast,
        icon = Icons.Default.Person
    ) {
        fun createRoute(scholarId: String) = "vietnam_scholar_detail/$scholarId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Quran,
    Screen.Tracker,
    Screen.Utilities,
    Screen.Settings
)
