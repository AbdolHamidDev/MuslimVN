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
    NavHost(
        navController = navController,
        startDestination = Screen.Ibadah.route,
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
        composable(Screen.Ibadah.route) {
            HomeScreen(
                onQuranClick = { navController.navigate(Screen.Quran.route) },
                onQiblaClick = { navController.navigate(Screen.Qibla.route) },
                onHijriCalendarClick = { navController.navigate(Screen.HijriCalendar.route) },
                onNamesOfAllahClick = { navController.navigate(Screen.NamesOfAllah.route) },
                onZakatClick = { navController.navigate(Screen.Zakat.route) },
                onPodcastClick = { navController.navigate(Screen.PodcastHome.route) },
                onOpenFullPlayer = { navController.navigate(Screen.PodcastPlayer.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onFeatureClick = { feature ->
                    if (feature == "Azkar") {
                        navController.navigate(Screen.Azkar.route)
                    } else {
                        navController.navigate("coming_soon/$feature")
                    }
                }
            )
        }
        composable(Screen.Knowledge.route) {
            val category = remember {
                RoadmapData.getKnowledgeCategory(
                    onZakatClick = { navController.navigate(Screen.Zakat.route) },
                    onPodcastClick = { navController.navigate(Screen.PodcastHome.route) },
                    onFeatureClick = { title -> navController.navigate("coming_soon/$title") }
                )
            }
            RoadmapCategoryScreen(title = "Kiến thức", category = category)
        }
        composable(Screen.Utilities.route) {
            val category = remember {
                RoadmapData.getUtilitiesCategory(
                    onZakatClick = { navController.navigate(Screen.Zakat.route) },
                    onFeatureClick = { title -> navController.navigate("coming_soon/$title") }
                )
            }
            RoadmapCategoryScreen(title = "Tiện ích", category = category)
        }
        composable(Screen.Local.route) {
            val category = remember {
                RoadmapData.getLocalCategory(
                    onFeatureClick = { title -> navController.navigate("coming_soon/$title") }
                )
            }
            RoadmapCategoryScreen(title = "Local Việt Nam", category = category)
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
                onBackClick = { navController.popBackStack() },
                onSurahClick = { surahNumber ->
                    navController.navigate(Screen.SurahDetail.createRoute(surahNumber))
                },
                onSettingsClick = { navController.navigate(Screen.QuranSettings.route) },
                onOpenFullPlayer = { navController.navigate(Screen.PodcastPlayer.route) }
            )
        }
        composable(Screen.QuranSettings.route) {
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
                navController = navController,
                onNavigateToQuranSettings = { navController.navigate(Screen.QuranSettings.route) },
                onNavigateToPrayerNotifications = { navController.navigate(Screen.PrayerNotifications.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { 
                    navController.previousBackStackEntry?.savedStateHandle?.set("profile_updated", true)
                    navController.popBackStack() 
                }
            )
        }
        composable(
            route = Screen.SurahDetail.route,
            arguments = listOf(navArgument("surahNumber") { type = NavType.IntType })
        ) {
            SurahDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.QuranSettings.route) },
                onOpenFullPlayer = { navController.navigate(Screen.PodcastPlayer.route) }
            )
        }

        // ── Podcast học giả ─────────────────────────────────────────────────────
        composable(Screen.PodcastHome.route) {
            PodcastHomeScreen(
                onBackClick = { navController.popBackStack() },
                onScholarClick = { scholarId ->
                    navController.navigate(Screen.ScholarDetail.createRoute(scholarId))
                },
                onOpenFullPlayer = { navController.navigate(Screen.PodcastPlayer.route) }
            )
        }
        composable(
            route = Screen.ScholarDetail.route,
            arguments = listOf(navArgument(Screen.ScholarDetail.ARG_SCHOLAR_ID) { type = NavType.StringType })
        ) {
            ScholarDetailScreen(
                onBackClick = { navController.popBackStack() },
                onOpenFullPlayer = { navController.navigate(Screen.PodcastPlayer.route) }
            )
        }
        composable(Screen.PodcastPlayer.route) {
            PodcastPlayerScreen(onCloseClick = { navController.popBackStack() })
        }
    }
}
