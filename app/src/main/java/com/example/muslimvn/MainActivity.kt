package com.example.muslimvn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import javax.inject.Inject
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.muslimvn.core.navigation.MainNavigation
import com.example.muslimvn.core.navigation.LocalFloatingNavigationDockInset
import com.example.muslimvn.core.navigation.bottomNavItems
import com.example.muslimvn.presentation.viewmodels.SettingsViewModel
import com.example.muslimvn.ui.theme.MuslimVNTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(UnstableApi::class)
    @Inject
    lateinit var quranPlayerCoordinator: com.example.muslimvn.data.util.QuranPlayerCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Bật edge-to-edge TRƯỚC setContent {}: cửa sổ vẽ tràn cả status/navigation bar,
        // status bar trong suốt, nội dung app trôi liền mạch phía sau system indicators.
        enableEdgeToEdge()

        // Xin quyền thông báo ngay khi vào app (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.appTheme.collectAsState()

            MuslimVNTheme(themeMode = themeMode) {
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
    // 64dp dock + 12dp khoảng nổi + 16dp khoảng thao tác, cộng navigation-bar inset.
    val floatingDockContentInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() + 92.dp

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalFloatingNavigationDockInset provides if (showBottomBar) floatingDockContentInset else 0.dp
        ) {
            MainNavigation(navController = navController, modifier = Modifier.fillMaxSize())
        }

        // Dock là lớp phủ của root thay vì Scaffold.bottomBar, nên nội dung tiếp tục
        // chạy phía dưới nó và không tạo một dải phân cách ở cuối mỗi top-level screen.
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = showBottomBar,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FloatingNavigationDock(
                currentDestination = currentDestination,
                onNavigate = { screen ->
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

/**
 * Dock điều hướng cấp cao. Một pill duy nhất được đặt phía sau các item và
 * animate vị trí của nó, thay vì xoá pill cũ rồi tạo pill mới ở destination kế tiếp.
 */
@Composable
private fun FloatingNavigationDock(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (com.example.muslimvn.core.navigation.Screen) -> Unit
) {
    val destinationSelectedIndex = bottomNavItems.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }.coerceAtLeast(0)
    var selectedIndex by remember { mutableIntStateOf(destinationSelectedIndex) }
    val navigationScope = rememberCoroutineScope()
    var pendingNavigation by remember { mutableStateOf<Job?>(null) }

    // Đồng bộ lại khi route đổi do back/deep link; tab click tự đổi selectedIndex ngay,
    // còn navigation được commit sau khi motion của dock đã hoàn tất.
    LaunchedEffect(destinationSelectedIndex) {
        selectedIndex = destinationSelectedIndex
    }
    val selectionTransition = updateTransition(
        targetState = selectedIndex,
        label = "navigationSelection"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val unitWidth = maxWidth / (bottomNavItems.size + 1)
                val pillOffset = selectionTransition.animateDp(
                    transitionSpec = {
                        tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    },
                    label = "navigationPillOffset"
                ) { index -> unitWidth * index }

                Box(
                    modifier = Modifier
                        .padding(start = pillOffset.value, top = 6.dp, bottom = 6.dp)
                        .width(unitWidth * 2)
                        .height(52.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        val selected = index == selectedIndex
                        val label = stringResource(screen.titleResId)
                        val itemWeight = selectionTransition.animateFloat(
                            transitionSpec = {
                                tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            },
                            label = "navigationItemWidth$index"
                        ) { selectedItemIndex ->
                            if (selectedItemIndex == index) 2f else 1f
                        }
                        Box(
                            modifier = Modifier
                                .weight(itemWeight.value)
                                .height(64.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .semantics { contentDescription = label }
                                .selectable(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            selectedIndex = index
                                            pendingNavigation?.cancel()
                                            pendingNavigation = navigationScope.launch {
                                                delay(180)
                                                onNavigate(screen)
                                            }
                                        }
                                    },
                                    role = Role.Tab
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (selected) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(start = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
