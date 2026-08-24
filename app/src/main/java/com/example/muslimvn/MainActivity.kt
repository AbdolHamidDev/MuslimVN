package com.example.muslimvn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.muslimvn.core.navigation.MainNavigation
import com.example.muslimvn.core.navigation.bottomNavItems
import com.example.muslimvn.ui.theme.MuslimVNTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Bật edge-to-edge TRƯỚC setContent {}: cửa sổ vẽ tràn cả status/navigation bar,
        // status bar trong suốt, nội dung app trôi liền mạch phía sau system indicators.
        enableEdgeToEdge()
        setContent {
            MuslimVNTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom bar chỉ hiển thị ở các destination cấp cao (5 tab chính),
    // tự động ẩn ở các màn hình con (Quran, Qibla, Lịch Hijri, chi tiết Surah)
    // để tối đa không gian nội dung — đúng chuẩn điều hướng Material 3.
    val topLevelRoutes = remember { bottomNavItems.map { it.route }.toSet() }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    Scaffold(
        // ⚡ FIX khoảng trắng kép phía trên TopAppBar (lỗi toàn app):
        // Root Scaffold này KHÔNG có topBar, nếu để mặc định contentWindowInsets =
        // WindowInsets.systemBars thì innerPadding.top = chiều cao status bar và bị
        // áp vào TOÀN BỘ màn hình trong NavHost. Trong khi đó TopAppBar của từng màn
        // lại tự trừ WindowInsets.statusBars lần nữa → status bar bị tính 2 lần.
        // → Root chỉ là shell điều hướng, KHÔNG nạp inset hệ thống; mỗi màn hình
        //   tự xử lý inset qua Scaffold/TopAppBar của chính nó (single source of truth).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.titleResId)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        MainNavigation(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}
