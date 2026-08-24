package com.example.muslimvn.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.muslimvn.R

sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object News : Screen("news", R.string.nav_news, Icons.Default.Newspaper)
    object Community : Screen("community", R.string.nav_community, Icons.Default.Groups)
    object AI : Screen("ai", R.string.nav_ai, Icons.Default.AutoAwesome)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    object Quran : Screen("quran", R.string.nav_quran, Icons.Default.Menu)
    object Qibla : Screen("qibla", R.string.nav_qibla, Icons.Default.LocationOn)
    object HijriCalendar : Screen("hijri_calendar", R.string.nav_hijri_calendar, Icons.Default.Schedule)
    object NamesOfAllah : Screen("names_of_allah", R.string.nav_names_of_allah, Icons.AutoMirrored.Filled.MenuBook)
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
    Screen.Home,
    Screen.News,
    Screen.Community,
    Screen.AI,
    Screen.Settings
)
