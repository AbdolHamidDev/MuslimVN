package com.example.muslimvn.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.muslimvn.R

sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Ibadah : Screen("ibadah", R.string.nav_ibadah, Icons.AutoMirrored.Filled.MenuBook)
    object Knowledge : Screen("knowledge", R.string.nav_knowledge, Icons.AutoMirrored.Filled.LibraryBooks)
    object Utilities : Screen("utilities", R.string.nav_utilities, Icons.Default.Widgets)
    object Local : Screen("local", R.string.nav_local, Icons.Default.LocationOn)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Profile : Screen("profile", R.string.nav_settings, Icons.Default.AccountCircle)

    object Azkar : Screen("azkar", R.string.utility_azkar, Icons.Default.Menu)
    object Quran : Screen("quran", R.string.nav_quran, Icons.Default.Menu)
    object Qibla : Screen("qibla", R.string.nav_qibla, Icons.Default.LocationOn)
    object HijriCalendar : Screen("hijri_calendar", R.string.nav_hijri_calendar, Icons.Default.Schedule)
    object NamesOfAllah : Screen("names_of_allah", R.string.nav_names_of_allah, Icons.AutoMirrored.Filled.MenuBook)
    object Zakat : Screen("zakat", R.string.utility_zakat, Icons.Default.Schedule)
    object QuranSettings : Screen("quran_settings", R.string.nav_settings, Icons.Default.Settings)
    object PrayerNotifications : Screen("prayer_notifications", R.string.nav_settings, Icons.Default.Notifications)
    object SurahDetail : Screen("surah_detail/{surahNumber}", R.string.nav_surah_detail, Icons.Default.Menu) {
        fun createRoute(surahNumber: Int) = "surah_detail/$surahNumber"
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
}

val bottomNavItems = listOf(
    Screen.Ibadah,
    Screen.Knowledge,
    Screen.Utilities,
    Screen.Local,
    Screen.Settings
)
