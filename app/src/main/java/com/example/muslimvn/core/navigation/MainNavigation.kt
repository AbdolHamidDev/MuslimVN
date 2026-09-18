package com.example.muslimvn.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.muslimvn.presentation.RoadmapData
import com.example.muslimvn.presentation.screens.*
import com.example.muslimvn.presentation.screens.zakat.ZakatScreen
import com.example.muslimvn.presentation.viewmodels.*

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
    val popBack: () -> Unit = { backStack.removeLastOrNull() }
    val openFullPlayer: (String?) -> Unit = { mediaId ->
        val parts = mediaId?.split(":")
        val destination = parts?.firstOrNull()?.toIntOrNull()?.let {
            Destination.SurahDetail(it, parts.getOrNull(1)?.toIntOrNull() ?: 1)
        } ?: Destination.PodcastPlayer
        backStack.add(destination)
    }

    NavDisplay(
        backStack = backStack, modifier = modifier, onBack = popBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Destination.Onboarding> {
                val viewModel = hiltViewModel<OnboardingViewModel>()
                OnboardingScreen(
                    onFinishOnboarding = {
                        viewModel.completeOnboarding()
                        backStack.clear()
                        backStack.add(Destination.Home)
                    }
                )
            }
            entry<Destination.Home> {
                HomeScreen(
                    onPodcastClick = { backStack.add(Destination.PodcastHome) },
                    onScholarClick = { backStack.add(Destination.ScholarDetail(it)) },
                    onVietnamScholarClick = { backStack.add(Destination.VietnamScholarDetail(it)) },
                    onDailyReminderClick = { backStack.add(Destination.DailyReminder(it)) },
                    onOpenFullPlayer = { openFullPlayer(null) },
                    onMasjidClick = {},
                    onHijriCalendarClick = { backStack.add(Destination.HijriCalendar) }
                )
            }
            entry<Destination.DailyReminder> { key -> DailyReminderViewerScreen(key.hadithId, popBack) }
            entry<Destination.Knowledge> { RoadmapCategoryScreen("Kiến thức", RoadmapData.getKnowledgeCategory { backStack.add(Destination.PodcastHome) }) }
            entry<Destination.Local> { RoadmapCategoryScreen("Local Việt Nam", RoadmapData.getLocalCategory()) }
            entry<Destination.Utilities> {
                UtilitiesScreen(
                    onPrayerTimesClick = {}, onQiblaClick = { backStack.add(Destination.Qibla) },
                    onHijriCalendarClick = { backStack.add(Destination.HijriCalendar) },
                    onNamesOfAllahClick = { backStack.add(Destination.NamesOfAllah) },
                    onZakatClick = { backStack.add(Destination.Zakat) },
                    onPodcastClick = { backStack.add(Destination.PodcastHome) },
                    onFeatureClick = { if (it == "Azkar") backStack.add(Destination.Azkar) }
                )
            }
            entry<Destination.Tracker> { TrackerScreen(onQuranClick = { surah, ayah -> backStack.add(Destination.SurahDetail(surah, ayah)) }) }
            entry<Destination.Azkar> { AzkarScreen(onBackClick = popBack) }
            entry<Destination.Quran> {
                QuranScreen({ surah, ayah -> backStack.add(Destination.SurahDetail(surah, ayah)) }, { backStack.add(Destination.QuranSettings()) }, openFullPlayer)
            }
            entry<Destination.QuranSettings> { key ->
                val viewModel = hiltViewModel<QuranSettingsViewModel, QuranSettingsViewModel.Factory> { it.create(key.surahNumber) }
                QuranSettingsScreen(popBack, viewModel)
            }
            entry<Destination.PrayerNotifications> {
                PrayerNotificationsScreen(
                    onBackClick = popBack,
                    onNavigateToCalculationDetails = { backStack.add(Destination.PrayerCalculationDetails) }
                )
            }
            entry<Destination.PrayerCalculationDetails> { PrayerCalculationDetailsScreen(onBackClick = popBack) }
            entry<Destination.Qibla> { QiblaScreen(popBack) }
            entry<Destination.HijriCalendar> { HijriCalendarScreen(popBack) }
            entry<Destination.NamesOfAllah> { NamesOfAllahScreen(popBack) }
            entry<Destination.Zakat> { ZakatScreen(popBack) }
            entry<Destination.Settings> {
                SettingsScreen(
                    onNavigateToQuranSettings = { backStack.add(Destination.QuranSettings()) },
                    onNavigateToPrayerNotifications = { backStack.add(Destination.PrayerNotifications) },
                    onNavigateToDownloadedVideos = { backStack.add(Destination.DownloadedVideos) },
                    onNavigateToDownloadedPodcasts = { backStack.add(Destination.DownloadedPodcasts) }
                )
            }
            entry<Destination.DownloadedPodcasts> {
                DownloadedPodcastsScreen(
                    onBackClick = popBack,
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.DownloadedVideos> {
                DownloadedVideosScreen(
                    onBackClick = popBack,
                    onVideoClick = { file, title, uploader -> backStack.add(Destination.YoutubePlayer(file, title, uploader)) },
                    onAudioPlayerClick = { backStack.add(Destination.PodcastPlayer) }
                )
            }
            entry<Destination.SurahDetail> { key ->
                val viewModel = hiltViewModel<SurahDetailViewModel, SurahDetailViewModel.Factory> { it.create(key.surahNumber, key.startAyah) }
                SurahDetailScreen(popBack, { backStack.add(Destination.QuranSettings(key.surahNumber)) }, { openFullPlayer(null) }, viewModel)
            }
            entry<Destination.PodcastHome> {
                PodcastHomeScreen(
                    onBackClick = popBack,
                    onLibraryClick = { backStack.add(Destination.PodcastLibrary) },
                    onScholarClick = { backStack.add(Destination.ScholarDetail(it)) },
                    onSeeAllMuslimCentralClick = { backStack.add(Destination.MuslimCentralScholars) },
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.PodcastLibrary> {
                PodcastLibraryScreen(
                    onBackClick = popBack,
                    onNavigateToFavorites = { backStack.add(Destination.PodcastFavorites) },
                    onNavigateToPlaylist = { backStack.add(Destination.PodcastPlaylist) },
                    onNavigateToDownloadedPodcasts = { backStack.add(Destination.DownloadedPodcasts) },
                    onNavigateToFollowed = { backStack.add(Destination.PodcastFollowed) },
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.PodcastFavorites> {
                PodcastFavoritesScreen(
                    onBackClick = popBack,
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.PodcastPlaylist> {
                PodcastPlaylistScreen(
                    onBackClick = popBack,
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.PodcastFollowed> {
                PodcastFollowedScreen(
                    onBackClick = popBack,
                    onScholarClick = { backStack.add(Destination.ScholarDetail(it)) },
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.MuslimCentralScholars> {
                com.example.muslimvn.presentation.screens.podcast.MuslimCentralScholarsScreen(
                    onBackClick = popBack,
                    onLibraryClick = { backStack.add(Destination.PodcastLibrary) },
                    onScholarClick = { backStack.add(Destination.ScholarDetail(it)) },
                    onOpenFullPlayer = { openFullPlayer(null) }
                )
            }
            entry<Destination.ScholarDetail> { key ->
                val viewModel = hiltViewModel<ScholarDetailViewModel, ScholarDetailViewModel.Factory> { it.create(key.scholarId) }
                ScholarDetailScreen(
                    onBackClick = popBack,
                    onLibraryClick = { backStack.add(Destination.PodcastLibrary) },
                    onOpenFullPlayer = { openFullPlayer(null) },
                    viewModel = viewModel
                )
            }
            entry<Destination.PodcastPlayer> { PodcastPlayerScreen(popBack) }
            entry<Destination.VietnamScholarDetail> { key ->
                VietnamScholarDetailScreen(
                    scholarId = key.scholarId, onBackClick = popBack,
                    onVideoClick = { url, title, channel, channelUrl -> backStack.add(Destination.YoutubePlayer(url, title, channel, channelUrl)) },
                    onDocumentClick = { url, title -> backStack.add(Destination.DocumentReader(url, title)) },
                    onOpenFullPlayer = { backStack.add(Destination.PodcastPlayer) }
                )
            }
            entry<Destination.YoutubePlayer> { key ->
                val viewModel = hiltViewModel<YoutubePlayerViewModel, YoutubePlayerViewModel.Factory> { it.create(key.videoUrl, key.videoTitle, key.channelName, key.channelUrl) }
                YoutubePlayerDetailScreen(popBack, viewModel)
            }
            entry<Destination.DocumentReader> { key -> DocumentReaderScreen(key.url, key.title, popBack) }
            // Temporarily hidden for v1 release scope
            // entry<Destination.MasjidList> { MasjidListScreen(onBackClick = popBack) }
        }
    )
}
