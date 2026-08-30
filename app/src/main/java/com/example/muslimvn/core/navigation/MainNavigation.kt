package com.example.muslimvn.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.muslimvn.R
import com.example.muslimvn.presentation.RoadmapData
import com.example.muslimvn.presentation.components.ComingSoonScreen
import com.example.muslimvn.presentation.screens.*
import com.example.muslimvn.presentation.screens.zakat.ZakatScreen

@Composable
fun MainNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    val openFullPlayer: (String?) -> Unit = { mediaId ->
        if (mediaId?.contains(":") == true) {
            val parts = mediaId.split(":")
            val surahNum = parts[0].toIntOrNull() ?: 1
            val ayahNum = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1
            navController.navigate(Screen.SurahDetail.createRoute(surahNum, ayahNum))
        } else {
            navController.navigate(Screen.PodcastPlayer.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        // Chuyển cảnh nhẹ nhàng: màn mới trượt lên + mờ dần hiện ra;
        // khi quay lại thì trượt xuống. Ngắn (<300ms) để không gây cảm giác chậm.
        enterTransition = {
            fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 20 }
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutVertically(tween(260)) { it / 20 }
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onPodcastClick = { navController.navigate(Screen.PodcastHome.route) },
                onScholarClick = { scholarId ->
                    navController.navigate(Screen.ScholarDetail.createRoute(scholarId))
                },
                onVietnamScholarClick = { scholarId ->
                    navController.navigate(Screen.VietnamScholarDetail.createRoute(scholarId))
                },
                onOpenFullPlayer = { openFullPlayer(null) }
            )
        }
        composable(Screen.Knowledge.route) {
            val category = remember {
                RoadmapData.getKnowledgeCategory(
                    onPodcastClick = { navController.navigate(Screen.PodcastHome.route) }
                )
            }
            RoadmapCategoryScreen(title = "Kiến thức", category = category)
        }
        composable(Screen.Utilities.route) {
            UtilitiesScreen(
                onPrayerTimesClick = { /* Không điều hướng, UtilitiesScreen tự hiện BottomSheet */ },
                onQiblaClick = { navController.navigate(Screen.Qibla.route) },
                onHijriCalendarClick = { navController.navigate(Screen.HijriCalendar.route) },
                onNamesOfAllahClick = { navController.navigate(Screen.NamesOfAllah.route) },
                onZakatClick = { navController.navigate(Screen.Zakat.route) },
                onPodcastClick = { navController.navigate(Screen.PodcastHome.route) },
                onFeatureClick = { feature ->
                    if (feature == "Azkar") {
                        navController.navigate(Screen.Azkar.route)
                    }
                }
            )
        }
        composable(Screen.Local.route) {
            val category = remember {
                RoadmapData.getLocalCategory()
            }
            RoadmapCategoryScreen(title = "Local Việt Nam", category = category)
        }
        composable(Screen.Tracker.route) {
            TrackerScreen(
                onQuranClick = { surahNumber, ayahNumber ->
                    navController.navigate(Screen.SurahDetail.createRoute(surahNumber, ayahNumber))
                }
            )
        }
        composable("coming_soon/{title}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ComingSoonScreen(title = title, onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Azkar.route) {
            AzkarScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Quran.route) {
            QuranScreen(
                onSurahClick = { surahNumber, ayahNumber ->
                    navController.navigate(Screen.SurahDetail.createRoute(surahNumber, ayahNumber))
                },
                onSettingsClick = { navController.navigate(Screen.QuranSettings.createRoute()) },
                onOpenFullPlayer = openFullPlayer
            )
        }
        composable(
            route = Screen.QuranSettings.route,
            arguments = listOf(navArgument("surahNumber") { type = NavType.IntType; defaultValue = -1 })
        ) {
            QuranSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.PrayerNotifications.route) {
            PrayerNotificationsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Qibla.route) {
            QiblaScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.HijriCalendar.route) {
            HijriCalendarScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.NamesOfAllah.route) {
            NamesOfAllahScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Zakat.route) {
            ZakatScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToQuranSettings = { navController.navigate(Screen.QuranSettings.route) },
                onNavigateToPrayerNotifications = { navController.navigate(Screen.PrayerNotifications.route) }
            )
        }
        composable(
            route = Screen.SurahDetail.route,
            arguments = listOf(
                navArgument("surahNumber") { type = NavType.IntType },
                navArgument("startAyah") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
            SurahDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.QuranSettings.createRoute(surahNumber)) },
                onOpenFullPlayer = { openFullPlayer(null) }
            )
        }

        // ── Podcast học giả ─────────────────────────────────────────────────────
        composable(Screen.PodcastHome.route) {
            PodcastHomeScreen(
                onBackClick = { navController.popBackStack() },
                onScholarClick = { scholarId ->
                    navController.navigate(Screen.ScholarDetail.createRoute(scholarId))
                },
                onOpenFullPlayer = { openFullPlayer(null) }
            )
        }
        composable(
            route = Screen.ScholarDetail.route,
            arguments = listOf(navArgument(Screen.ScholarDetail.ARG_SCHOLAR_ID) { type = NavType.StringType })
        ) {
            ScholarDetailScreen(
                onBackClick = { navController.popBackStack() },
                onOpenFullPlayer = { openFullPlayer(null) }
            )
        }
        composable(Screen.PodcastPlayer.route) {
            PodcastPlayerScreen(onCloseClick = { navController.popBackStack() })
        }

        composable(
            route = Screen.VietnamScholarDetail.route,
            arguments = listOf(navArgument("scholarId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scholarId = backStackEntry.arguments?.getString("scholarId") ?: ""
            VietnamScholarDetailScreen(
                scholarId = scholarId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
