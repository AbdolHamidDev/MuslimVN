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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.models.ReminderMode
import com.example.muslimvn.domain.util.HijriCalendarUtils
import com.example.muslimvn.domain.util.HijriMonthNames
import com.example.muslimvn.presentation.components.*
import com.example.muslimvn.presentation.viewmodels.HomeViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel(),
    onQuranClick: () -> Unit = {},
    onQiblaClick: () -> Unit = {},
    onHijriCalendarClick: () -> Unit = {},
    onNamesOfAllahClick: () -> Unit = {},
    onZakatClick: () -> Unit = {},
    onPodcastClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPrayerSheet by remember { mutableStateOf(false) }
    var showAllUtilitiesSheet by remember { mutableStateOf(false) }
    var selectedPrayerForReminder by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val notificationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (viewModel.syncPermissionState()) {
                    viewModel.refreshPrayerTimes()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PodcastPlayerBarState(playerViewModel) { active ->
                androidx.compose.animation.AnimatedVisibility(visible = active != null) {
                    if (active != null) {
                        val podcastPlaylist by playerViewModel.playlist.collectAsState()
                        val isQuran = active.id.contains(":")
                        
                        MiniPlayerBar(
                            title = active.title,
                            subtitle = active.subtitle,
                            artworkPath = active.artworkPath,
                            isPlaying = active.isPlaying,
                            isBuffering = active.isBuffering,
                            positionMs = active.positionMs,
                            durationMs = active.durationMs,
                            speedLabel = formatSpeedLabel(active.speed),
                            onPlayPauseClick = playerViewModel::togglePlayPause,
                            onSeekTo = playerViewModel::seekTo,
                            onCycleSpeed = playerViewModel::cyclePlaybackSpeed,
                            onOpenFullPlayer = onOpenFullPlayer,
                            currentMediaId = active.id,
                            playlist = if (isQuran) emptyList() else podcastPlaylist,
                            onPlayEpisode = playerViewModel::playEpisode
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            GreetingSection(
                userData = uiState.userData,
                userProfile = uiState.userProfile,
                onProfileClick = onSettingsClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection(hijriOffsetDays = hijriOffset)
            Spacer(modifier = Modifier.height(16.dp))

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

            val prayerTimes = uiState.prayerTimes
            if (prayerTimes != null) {
                NextPrayerHero(
                    prayerTimes = prayerTimes,
                    onCountdownFinished = viewModel::refreshPrayerTimes,
                    modifier = Modifier.bouncyClick {
                        selectedPrayerForReminder = prayerTimes.nextPrayerName
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.utilities_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAllUtilitiesSheet = true }) {
                        Text(text = stringResource(R.string.view_all))
                    }
                }

                val onUtilityClick: (UtilityItem) -> Unit = { item ->
                    when (item.id) {
                        "quran" -> onQuranClick()
                        "compass" -> onQiblaClick()
                        "prayer" -> {
                            showPrayerSheet = true
                        }
                        "schedule" -> onHijriCalendarClick()
                        "99" -> onNamesOfAllahClick()
                        "zakat" -> onZakatClick()
                        "podcast" -> onPodcastClick()
                        else -> scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.feature_coming_soon, item.name)
                            )
                        }
                    }
                }

                UtilityCarousel(onItemClick = onUtilityClick)

                if (showPrayerSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showPrayerSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.utility_prayer),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                            PrayerList(
                                prayerTimes = prayerTimes,
                                reminders = uiState.reminders,
                                onReminderClick = { 
                                    selectedPrayerForReminder = it 
                                }
                            )
                        }
                    }
                }

                if (showAllUtilitiesSheet) {
                    AllUtilitiesBottomSheet(
                        onDismiss = { showAllUtilitiesSheet = false },
                        onItemClick = { item ->
                            showAllUtilitiesSheet = false
                            onUtilityClick(item)
                        }
                    )
                }

                selectedPrayerForReminder?.let { prayerName ->
                    val reminder = uiState.reminders[prayerName] ?: PrayerReminder(prayerName)
                    PrayerReminderBottomSheet(
                        prayerName = prayerName,
                        currentReminder = reminder,
                        onDismiss = { 
                            selectedPrayerForReminder = null 
                        },
                        onSave = { 
                            viewModel.updateReminder(it)
                            selectedPrayerForReminder = null
                        }
                    )
                }
            } else {
                if (uiState.isLoading) {
                    LoadingIndicator(
                        label = stringResource(R.string.loading_please_wait),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ErrorState(
                        message = uiState.error ?: stringResource(R.string.prayer_times_error),
                        onRetry = viewModel::refreshPrayerTimes,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun UtilityCarousel(onItemClick: (UtilityItem) -> Unit) {
    val items = getUtilityItems()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items) { item ->
            Box(modifier = Modifier.width(85.dp)) {
                UtilityCard(item, onItemClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUtilitiesBottomSheet(
    onDismiss: () -> Unit,
    onItemClick: (UtilityItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.all_utilities_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            UtilityGrid(onItemClick = onItemClick)
        }
    }
}

private @Composable
fun getUtilityItems() = listOf(
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

@Composable
fun UtilityGrid(onItemClick: (UtilityItem) -> Unit) {
    val items = getUtilityItems()

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
    val gregorianDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val hijriLabel = remember(hijriOffsetDays) {
        HijriCalendarUtils.hijriDateFor(LocalDate.now(), hijriOffsetDays)?.let { hijri ->
            val day = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            val month = HijriMonthNames.monthName(hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR))
            val year = hijri.get(java.time.temporal.ChronoField.YEAR)
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
fun PrayerList(prayerTimes: PrayerTimes, reminders: Map<String, PrayerReminder>, onReminderClick: (String) -> Unit) {
    val prayers = listOf(
        PrayerItemData(stringResource(R.string.prayer_fajr), prayerTimes.fajr, Icons.Default.NightsStay, PrayerName.FAJR),
        PrayerItemData(stringResource(R.string.prayer_sunrise), prayerTimes.sunrise, Icons.Default.WbTwilight, PrayerName.SUNRISE),
        PrayerItemData(stringResource(R.string.prayer_dhuhr), prayerTimes.dhuhr, Icons.Default.WbSunny, PrayerName.DHUHR),
        PrayerItemData(stringResource(R.string.prayer_asr), prayerTimes.asr, Icons.Default.WbCloudy, PrayerName.ASR),
        PrayerItemData(stringResource(R.string.prayer_maghrib), prayerTimes.maghrib, Icons.Default.WbTwilight, PrayerName.MAGHRIB),
        PrayerItemData(stringResource(R.string.prayer_isha), prayerTimes.isha, Icons.Default.Bedtime, PrayerName.ISHA)
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(prayers) { prayer ->
            val reminder = reminders[prayer.id] ?: PrayerReminder(prayer.id)
            PrayerItemRow(prayer, reminder, onReminderClick)
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
    val icon: ImageVector,
    val id: String
)

@Composable
fun PrayerItemRow(prayer: PrayerItemData, reminder: PrayerReminder, onReminderClick: (String) -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isEnabled = reminder.mode != ReminderMode.SILENT
    val backgroundImage = getPrayerImage(prayer.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onReminderClick(prayer.id) },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = backgroundImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = prayer.icon, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = prayer.name, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeFormat.format(prayer.time), 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(
                    onClick = { onReminderClick(prayer.id) },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = when (reminder.mode) {
                            ReminderMode.SILENT -> Icons.Default.NotificationsOff
                            ReminderMode.NOTIFICATION -> Icons.Default.Notifications
                            ReminderMode.ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = stringResource(R.string.toggle_adhan),
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun getPrayerImage(prayerId: String): String {
    val base = "file:///android_asset/images/praytime/"
    return when (prayerId) {
        PrayerName.FAJR -> "${base}fajr.webp"
        PrayerName.DHUHR -> "${base}dhuhr.jpg"
        PrayerName.ASR -> "${base}asr.jpg"
        PrayerName.MAGHRIB -> "${base}maghrib.jpg"
        PrayerName.ISHA -> "${base}isha.jpg"
        else -> "${base}vietnammosque.jpg"
    }
}

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

private fun shouldShowLocationRationale(context: Context): Boolean =
    (context as? Activity)
        ?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
