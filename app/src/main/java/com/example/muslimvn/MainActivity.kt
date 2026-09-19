package com.example.muslimvn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.muslimvn.core.navigation.Destination
import com.example.muslimvn.core.navigation.MainNavigation
import com.example.muslimvn.core.navigation.topLevelDestinations
import com.example.muslimvn.presentation.viewmodels.OnboardingViewModel
import com.example.muslimvn.presentation.viewmodels.SettingsViewModel
import com.example.muslimvn.ui.theme.MuslimVNTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.appTheme.collectAsState()
            val useDynamicColor by settingsViewModel.useDynamicColor.collectAsState()
            MuslimVNTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) { MainScreen() }
        }
    }
}

@Composable
fun MainScreen() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()

    if (isOnboardingCompleted == null) {
        return
    }

    val startDestination = if (isOnboardingCompleted == true) Destination.Home else Destination.Onboarding
    val backStack = rememberNavBackStack(startDestination)
    val currentDestination = backStack.lastOrNull()
    val showNavigationBar = currentDestination in topLevelDestinations.map { it.destination }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNavigationBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    topLevelDestinations.forEach { item ->
                        val selected = currentDestination == item.destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    backStack.clear()
                                    backStack.add(item.destination)
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId)) },
                            label = { Text(stringResource(item.titleResId)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        MainNavigation(backStack = backStack, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
