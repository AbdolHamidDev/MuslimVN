package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.core.navigation.LocalFloatingNavigationDockInset
import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.presentation.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToQuranSettings: () -> Unit,
    onNavigateToPrayerNotifications: () -> Unit,
    onNavigateToDownloadedVideos: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val appTheme by viewModel.appTheme.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalFloatingNavigationDockInset.current)
            ) {
                item { PreferenceHeader(title = "Giao diện") }
                item {
                    PreferenceItem(
                        title = "Chế độ tối",
                        subtitle = when (appTheme) {
                            AppTheme.FOLLOW_SYSTEM -> "Theo hệ thống"
                            AppTheme.LIGHT -> "Sáng"
                            AppTheme.DARK -> "Tối"
                        },
                        icon = Icons.Default.DarkMode,
                        onClick = { showThemeDialog = true }
                    )
                }

                item { PreferenceHeader(title = "Nội dung & Thông báo") }
                item {
                    PreferenceItem(
                        title = "Nội dung đã tải về",
                        subtitle = "Quản lý và nghe/xem lại video, podcast offline",
                        icon = Icons.Default.Download,
                        onClick = onNavigateToDownloadedVideos
                    )
                }
                item {
                    PreferenceItem(
                        title = "Cài đặt Quran",
                        subtitle = "Font chữ, học giả, chế độ hiển thị",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToQuranSettings
                    )
                }
                item {
                    PreferenceItem(
                        title = "Thông báo cầu nguyện",
                        subtitle = "Âm thanh Adhan và thông báo",
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToPrayerNotifications
                    )
                }

                item { PreferenceHeader(title = "Về ứng dụng") }
                item {
                    PreferenceItem(
                        title = "Đánh giá ứng dụng",
                        icon = Icons.Default.Star,
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("market://details?id=${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                })
                            }
                        }
                    )
                }
                item {
                    val packageInfo = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val version = packageInfo?.versionName ?: "1.0.0"
                    PreferenceItem(
                        title = "Phiên bản",
                        subtitle = version,
                        icon = Icons.Default.Info,
                        onClick = { }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = appTheme,
            onThemeSelected = {
                viewModel.onThemeSelected(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn chế độ tối") },
        text = {
            Column {
                ThemeOption(
                    title = "Theo hệ thống",
                    selected = currentTheme == AppTheme.FOLLOW_SYSTEM,
                    onClick = { onThemeSelected(AppTheme.FOLLOW_SYSTEM) }
                )
                ThemeOption(
                    title = "Sáng",
                    selected = currentTheme == AppTheme.LIGHT,
                    onClick = { onThemeSelected(AppTheme.LIGHT) }
                )
                ThemeOption(
                    title = "Tối",
                    selected = currentTheme == AppTheme.DARK,
                    onClick = { onThemeSelected(AppTheme.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PreferenceHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 8.dp)
    )
}

@Composable
fun PreferenceItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
