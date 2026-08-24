package com.example.muslimvn.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.util.HijriCalendarUtils
import com.example.muslimvn.domain.util.HijriMonthNames
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onQuranClick: () -> Unit = {},
    onQiblaClick: () -> Unit = {},
    onHijriCalendarClick: () -> Unit = {},
    onNamesOfAllahClick: () -> Unit = {},
    onPodcastClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPrayerSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Xin vị trí + thông báo trong MỘT đợt duy nhất.
    // POST_NOTIFICATIONS chỉ tồn tại từ Android 13 (API 33) trở lên.
    val notificationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    // Số lần người dùng từ chối quyền vị trí — dùng để phát hiện trường hợp
    // "Không hỏi lại" (từ chối vĩnh viễn) và chuyển sang hướng dẫn mở Cài đặt.
    var deniedAttempts by rememberSaveable { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!locationGranted) deniedAttempts++
        val notificationsGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                grants[Manifest.permission.POST_NOTIFICATIONS] == true
        viewModel.onPermissionsResult(locationGranted, notificationsGranted)
    }

    val hijriOffset by viewModel.hijriDateOffset.collectAsState()

    // Kiểm tra lại quyền mỗi khi màn hình RESUME (cả lần đầu vào): phủ trường hợp
    // người dùng bật quyền thủ công trong Cài đặt rồi quay về — card phải tự biến mất.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (viewModel.syncPermissionState()) {
                    // Vừa mới có quyền vị trí → tính lại giờ cầu nguyện theo vị trí thật
                    viewModel.refreshPrayerTimes()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        // Chỉ cần inset status bar (màn không có TopAppBar). Phía đáy NavigationBar
        // của root Scaffold (MainActivity) đã chịu trách nhiệm — nếu dùng mặc định
        // systemBars thì navigation bar bị tính 2 lần, nội dung bị hụt chiều cao.
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            HeaderSection(hijriOffsetDays = hijriOffset)
            Spacer(modifier = Modifier.height(16.dp))

            // Card giải thích lý do cần vị trí — hiện khi CHƯA có quyền và người
            // dùng chưa bấm "Để sau". Chỉ gọi hộp thoại hệ thống khi người dùng
            // chủ động bấm "Cấp quyền" (UX tốt hơn auto-prompt khi mở app).
            if (!uiState.isLocationPermissionGranted && !uiState.isPermissionCardDismissed) {
                LocationPermissionCard(
                    isPermanentlyDenied = deniedAttempts > 0 &&
                        !shouldShowLocationRationale(context),
                    onGrantClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                *notificationPermissions
                            )
                        )
                    },
                    onOpenSettingsClick = { openAppSettings(context) },
                    onDismissClick = viewModel::dismissPermissionCard
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            
            uiState.prayerTimes?.let { times ->
                NextPrayerCard(times, onCountdownFinished = viewModel::refreshPrayerTimes)
                Spacer(modifier = Modifier.height(24.dp))

                
                Text(
                    text = stringResource(R.string.utilities_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                UtilityGrid(
                    onItemClick = { item ->
                        when (item.id) {
                            "quran" -> onQuranClick()
                            "compass" -> onQiblaClick()
                            "prayer" -> showPrayerSheet = true
                            "schedule" -> onHijriCalendarClick()
                            "99" -> onNamesOfAllahClick()
                            "podcast" -> onPodcastClick()
                            // Tính năng chưa triển khai → phản hồi "sắp ra mắt" thay vì im lặng
                            else -> scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.feature_coming_soon, item.name)
                                )
                            }
                        }
                    }
                )

              
            } ?: run {
                if (uiState.isLoading) {
                    LoadingIndicator(
                        label = stringResource(R.string.loading_please_wait),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ErrorState(
                        message = stringResource(R.string.prayer_times_error),
                        onRetry = viewModel::refreshPrayerTimes,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun UtilityGrid(onItemClick: (UtilityItem) -> Unit) {
    val items = listOf(
        UtilityItem("quran", stringResource(R.string.utility_quran), "file:///android_asset/icon/quran.png"),
        UtilityItem("compass", stringResource(R.string.utility_qibla), "file:///android_asset/icon/compass.png"),
        UtilityItem("prayer", stringResource(R.string.utility_prayer), "file:///android_asset/icon/prayer.png"),
        UtilityItem("schedule", stringResource(R.string.utility_schedule), "file:///android_asset/icon/schedule.png"),
        UtilityItem("99", stringResource(R.string.utility_99_names), "file:///android_asset/icon/99.png"),
        UtilityItem("tasbih", stringResource(R.string.utility_tasbih), "file:///android_asset/icon/tasbih.png"),
        UtilityItem("doa", stringResource(R.string.utility_doa), "file:///android_asset/icon/doa.png"),
        UtilityItem("zakat", stringResource(R.string.utility_zakat), "file:///android_asset/icon/zakat.png"),
        UtilityItem("hadih", stringResource(R.string.utility_hadith), "file:///android_asset/icon/hadih.png"),
        UtilityItem("book", stringResource(R.string.utility_library), "file:///android_asset/icon/book.png"),
        UtilityItem("study", stringResource(R.string.utility_study), "file:///android_asset/icon/study.png"),
        UtilityItem("podcast", stringResource(R.string.utility_podcast), "file:///android_asset/icon/podcast.png"),
        UtilityItem("building", stringResource(R.string.utility_mosque), "file:///android_asset/icon/building.png"),
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items) { item ->
            UtilityCard(item, onItemClick)
        }
    }
}

@Composable
fun UtilityCard(item: UtilityItem, onClick: (UtilityItem) -> Unit) {
    // Icon TIỆN ÍCH hiển thị ẢNH TRẦN: không Card, không nền, không viền/shadow
    // bao ngoài. Chỉ giữ phản hồi nhấn scale spring + haptic nhẹ cho đã tay.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .bouncyClick(pressedScale = 0.92f) { onClick(item) }
            .padding(4.dp)
    ) {
        AsyncImage(
            model = item.iconUrl,
            contentDescription = item.name,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun HeaderSection(hijriOffsetDays: Int) {
    // remember để không tạo lại formatter (và format lại chuỗi) mỗi lần recompose
    val gregorianDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Áp dụng offset người dùng điều chỉnh ở màn Lịch Hijri (qua cùng DataStore)
    // để header Trang chủ và màn Lịch Hijri luôn hiển thị khớp nhau.
    val hijriLabel = remember(hijriOffsetDays) {
        HijriCalendarUtils.hijriDateFor(LocalDate.now(), hijriOffsetDays)?.let { hijri ->
            val day = hijri.get(ChronoField.DAY_OF_MONTH)
            val month = HijriMonthNames.monthName(hijri.get(ChronoField.MONTH_OF_YEAR))
            val year = hijri.get(ChronoField.YEAR)
            "$day $month $year AH"
        }.orEmpty()
    }

    Column {
        Text(
            text = gregorianDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = hijriLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun NextPrayerCard(prayerTimes: PrayerTimes, onCountdownFinished: () -> Unit) {
    // Đếm ngược theo GIÂY ngay trong UI (không gọi lại repository mỗi giây).
    // Khi hết giờ → yêu cầu tính lại mốc cầu nguyện tiếp theo đúng một lần.
    var remainingMs by remember(prayerTimes.nextPrayerTime) {
        mutableLongStateOf(prayerTimes.nextPrayerTime.time - System.currentTimeMillis())
    }
    var refreshRequested by remember(prayerTimes.nextPrayerTime) { mutableStateOf(false) }

    LaunchedEffect(prayerTimes.nextPrayerTime) {
        while (true) {
            remainingMs = prayerTimes.nextPrayerTime.time - System.currentTimeMillis()
            if (!refreshRequested && remainingMs <= 0L) {
                refreshRequested = true
                onCountdownFinished()
            }
            delay(1_000)
        }
    }

    // ── HERO CARD duy nhất của Trang chủ ──────────────────────────────────────
    // Nền solid primary nổi bật trên canvas trắng sạch (quy tắc "1 Hero Card"),
    // mọi card khác trên màn hình đều nền trắng không màu.
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tên lời nguyện trượt lên/xuống mượt mà bằng spring khi mốc thay đổi
            AnimatedContent(
                targetState = prayerTimes.nextPrayerName,
                transitionSpec = {
                    val offsetSpec = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)
                    val fadeSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                    (
                        slideInVertically(offsetSpec) { it / 2 } + fadeIn(fadeSpec)
                        ).togetherWith(slideOutVertically(offsetSpec) { -it / 2 } + fadeOut(fadeSpec))
                },
                label = "nextPrayerName"
            ) { name ->
                Text(
                    text = stringResource(R.string.next_prayer_label, name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatRemaining(remainingMs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.countdown_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

/** Định dạng "HH:mm:ss" cho thời gian còn lại tới mốc cầu nguyện tiếp theo. */
private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
fun PrayerList(prayerTimes: PrayerTimes) {
    val prayers = listOf(
        PrayerItemData(stringResource(R.string.prayer_fajr), prayerTimes.fajr, Icons.Default.Schedule),
        PrayerItemData(stringResource(R.string.prayer_sunrise), prayerTimes.sunrise, Icons.Default.Schedule),
        PrayerItemData(stringResource(R.string.prayer_dhuhr), prayerTimes.dhuhr, Icons.Default.Schedule),
        PrayerItemData(stringResource(R.string.prayer_asr), prayerTimes.asr, Icons.Default.Schedule),
        PrayerItemData(stringResource(R.string.prayer_maghrib), prayerTimes.maghrib, Icons.Default.Schedule),
        PrayerItemData(stringResource(R.string.prayer_isha), prayerTimes.isha, Icons.Default.Schedule)
    )

    LazyColumn {
        items(prayers) { prayer ->
            PrayerItemRow(prayer)
        }
    }
}

data class UtilityItem(
    val id: String,
    val name: String,
    val iconUrl: String
)

data class PrayerItemData(
    val name: String,
    val time: Date,
    val icon: ImageVector
)

@Composable
fun PrayerItemRow(prayer: PrayerItemData) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var isEnabled by remember { mutableStateOf(true) }

    // Hàng phẳng phân tách bằng divider mảnh thay vì Surface đóng khung từng dòng
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(prayer.icon, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = prayer.name, fontWeight = FontWeight.Bold)
                    Text(text = timeFormat.format(prayer.time), style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { isEnabled = !isEnabled }) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = stringResource(R.string.toggle_adhan),
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}

/**
 * Card giải thích lý do cần quyền vị trí TRƯỚC khi gọi hộp thoại hệ thống.
 * Khi bị từ chối vĩnh viễn ("Không hỏi lại") thì chuyển sang hướng dẫn mở Cài đặt.
 */
@Composable
fun LocationPermissionCard(
    isPermanentlyDenied: Boolean,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        // Nền trắng tinh + viền hairline rất nhạt — không dùng container màu đậm
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.permission_location_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPermanentlyDenied) {
                    stringResource(R.string.permission_location_denied)
                } else {
                    stringResource(R.string.permission_location_rationale)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPermanentlyDenied) {
                    FilledTonalButton(onClick = onOpenSettingsClick) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                } else {
                    Button(onClick = onGrantClick) {
                        Text(stringResource(R.string.action_grant_permission))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissClick) {
                    Text(stringResource(R.string.action_later))
                }
            }
        }
    }
}

/** True khi hệ thống VẪN có thể hiện lại dialog xin quyền (chưa bị từ chối vĩnh viễn). */
private fun shouldShowLocationRationale(context: Context): Boolean =
    (context as? Activity)
        ?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true

/** Mở trang thông tin ứng dụng trong Cài đặt để người dùng bật quyền thủ công. */
private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
