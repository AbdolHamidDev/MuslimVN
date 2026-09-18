@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.muslimvn.presentation.viewmodels.DailyReminderViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel(),
    onPodcastClick: () -> Unit = {},
    onScholarClick: (String) -> Unit = {},
    onVietnamScholarClick: (String) -> Unit = {},
    onDailyReminderClick: (String) -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    onMasjidClick: () -> Unit = {},
    onHijriCalendarClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dailyReminderViewModel: DailyReminderViewModel = hiltViewModel()
    val dailyReminderState by dailyReminderViewModel.uiState.collectAsStateWithLifecycle()
    var showPrayerSheet by remember { mutableStateOf(false) }
    var selectedPrayerForReminder by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hijriOffset by viewModel.hijriDateOffset.collectAsStateWithLifecycle()

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
                val isPodcast = active != null && !active.id.contains(":") && !active.id.startsWith("islamhouse_")
                androidx.compose.animation.AnimatedVisibility(visible = isPodcast) {
                    if (active != null) {
                        val podcastPlaylist by playerViewModel.playlist.collectAsStateWithLifecycle()

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
                            playlist = podcastPlaylist,
                            onPlayEpisode = playerViewModel::playEpisode
                        )
                    }
                }
            }
        }
    ) { padding ->
        val prayerTimes = uiState.prayerTimes

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = padding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Next Prayer Hero Section (Always visible, show skeleton if null)
            item {
                if (prayerTimes != null) {
                    NextPrayerHero(
                        prayerTimes = prayerTimes,
                        hijriOffset = hijriOffset,
                        onCountdownFinished = viewModel::refreshPrayerTimes,
                        onMasjidClick = onMasjidClick,
                        onDateClick = onHijriCalendarClick,
                        userLocation = uiState.userLocationAddress,
                        modifier = Modifier.bouncyClick {
                            selectedPrayerForReminder = prayerTimes.nextPrayerName
                        }
                    )
                } else if (uiState.isLoading) {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(260.dp))
                } else if (uiState.error != null) {
                    ErrorState(
                        message = uiState.error ?: stringResource(R.string.prayer_times_error),
                        onRetry = viewModel::refreshPrayerTimes,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
            }

            // 2. Daily Reminder Section (Always show if state is available)
            item {
                DailyReminderSection(
                    state = dailyReminderState,
                    onStoryClick = { onDailyReminderClick(it.id) }
                )
            }

            // 3. Vietnam Scholars Section
            item {
                VietnamScholarsSection(
                    onScholarClick = { scholar ->
                        onVietnamScholarClick(scholar.id)
                    }
                )
            }

            // 4. Featured Podcast Section
            if (uiState.featuredScholars.isNotEmpty()) {
                item {
                    FeaturedPodcastSection(
                        scholars = uiState.featuredScholars,
                        onScholarClick = onScholarClick,
                        onSeeAllClick = onPodcastClick
                    )
                }
            } else if (uiState.isLoading) {
                item {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp))
                }
            }
        }

        // Bottom Sheets placed outside LazyColumn for instant UI response
        if (prayerTimes != null && showPrayerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPrayerSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(
                        text = stringResource(R.string.utility_prayer),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    PrayerList(
                        prayerTimes = prayerTimes,
                        reminders = uiState.reminders,
                        onReminderClick = { selectedPrayerForReminder = it }
                    )
                }
            }
        }

        selectedPrayerForReminder?.let { prayerName ->
            val reminder = uiState.reminders[prayerName] ?: PrayerReminder(prayerName)
            PrayerReminderBottomSheet(
                prayerName = prayerName,
                currentReminder = reminder,
                onDismiss = { selectedPrayerForReminder = null },
                onSave = { 
                    viewModel.updateReminder(it)
                    selectedPrayerForReminder = null
                }
            )
        }
    }
}



@Composable
fun FeaturedPodcastSection(
    scholars: List<com.example.muslimvn.domain.models.Scholar>,
    onScholarClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.podcast_featured_scholars),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                onClick = onSeeAllClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.view_all),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(scholars) { scholar ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(84.dp)
                        .bouncyClick { onScholarClick(scholar.id) }
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        AsyncImage(
                            model = scholar.avatarPath.toAndroidAssetUri(),
                            contentDescription = scholar.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scholar.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
