package com.example.muslimvn.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.muslimvn.R
import com.example.muslimvn.presentation.components.ComingSoonScreen
import com.example.muslimvn.presentation.screens.HijriCalendarScreen
import com.example.muslimvn.presentation.screens.HomeScreen
import com.example.muslimvn.presentation.screens.NamesOfAllahScreen
import com.example.muslimvn.presentation.screens.PodcastHomeScreen
import com.example.muslimvn.presentation.screens.PodcastPlayerScreen
import com.example.muslimvn.presentation.screens.QiblaScreen
import com.example.muslimvn.presentation.screens.QuranScreen
import com.example.muslimvn.presentation.screens.QuranSettingsScreen
import com.example.muslimvn.presentation.screens.ScholarDetailScreen
import com.example.muslimvn.presentation.screens.SettingsScreen
import com.example.muslimvn.presentation.screens.SurahDetailScreen
import com.example.muslimvn.presentation.screens.zakat.ZakatScreen

@Composable
fun MainNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
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
                onQuranClick = { navController.navigate(Screen.Quran.route) },
                onQiblaClick = { navController.navigate(Screen.Qibla.route) },
                onHijriCalendarClick = { navController.navigate(Screen.HijriCalendar.route) },
                onNamesOfAllahClick = { navController.navigate(Screen.NamesOfAllah.route) },
                onZakatClick = { navController.navigate(Screen.Zakat.route) },
                onPodcastClick = { navController.navigate(Screen.PodcastHome.route) },
                onOpenFullPlayer = { navController.navigate(Screen.PodcastPlayer.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.News.route) {
            ComingSoonScreen(title = stringResource(R.string.nav_news))
        }
        composable(Screen.Community.route) {
            ComingSoonScreen(title = stringResource(R.string.nav_community))
        }
        composable(Screen.AI.route) {
            ComingSoonScreen(title = stringResource(R.string.nav_ai))
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
        composable(Screen.Settings.route) { SettingsScreen() }
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
