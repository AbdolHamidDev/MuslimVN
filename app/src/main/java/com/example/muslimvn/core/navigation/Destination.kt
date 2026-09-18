package com.example.muslimvn.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.muslimvn.R
import kotlinx.serialization.Serializable

/** Type-safe, persistable destinations used directly by Navigation 3. */
@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Onboarding : Destination
    @Serializable data object Home : Destination
    @Serializable data object Quran : Destination
    @Serializable data object Tracker : Destination
    @Serializable data object Utilities : Destination
    @Serializable data object Settings : Destination
    @Serializable data object Knowledge : Destination
    @Serializable data object Local : Destination
    @Serializable data object Azkar : Destination
    @Serializable data object Qibla : Destination
    @Serializable data object HijriCalendar : Destination
    @Serializable data object NamesOfAllah : Destination
    @Serializable data object Zakat : Destination
    @Serializable data class QuranSettings(val surahNumber: Int = -1) : Destination
    @Serializable data object PrayerNotifications : Destination
    @Serializable data object PrayerCalculationDetails : Destination
    @Serializable data object DownloadedVideos : Destination
    @Serializable data object DownloadedPodcasts : Destination
    @Serializable data class SurahDetail(val surahNumber: Int, val startAyah: Int = 1) : Destination
    @Serializable data class DailyReminder(val hadithId: String) : Destination
    @Serializable data object PodcastHome : Destination
    @Serializable data object PodcastLibrary : Destination
    @Serializable data object PodcastFavorites : Destination
    @Serializable data object PodcastPlaylist : Destination
    @Serializable data object PodcastFollowed : Destination
    @Serializable data object MuslimCentralScholars : Destination
    @Serializable data class ScholarDetail(val scholarId: String) : Destination
    @Serializable data object PodcastPlayer : Destination
    @Serializable data class VietnamScholarDetail(val scholarId: String) : Destination
    @Serializable data class YoutubePlayer(val videoUrl: String, val videoTitle: String = "", val channelName: String = "", val channelUrl: String = "") : Destination
    @Serializable data class DocumentReader(val url: String, val title: String = "") : Destination
    @Serializable data object MasjidList : Destination
}

data class TopLevelDestination(val destination: Destination, @StringRes val titleResId: Int, val icon: ImageVector)

val topLevelDestinations = listOf(
    TopLevelDestination(Destination.Home, R.string.nav_home, Icons.Default.Home),
    TopLevelDestination(Destination.Quran, R.string.nav_quran, Icons.AutoMirrored.Filled.MenuBook),
    TopLevelDestination(Destination.Tracker, R.string.nav_tracker, Icons.Default.Analytics),
    TopLevelDestination(Destination.Utilities, R.string.nav_utilities, Icons.Default.Widgets),
    TopLevelDestination(Destination.Settings, R.string.nav_settings, Icons.Default.Settings)
)
