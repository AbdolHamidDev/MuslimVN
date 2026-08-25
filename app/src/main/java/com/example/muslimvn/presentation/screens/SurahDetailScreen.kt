package com.example.muslimvn.presentation.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.domain.models.Ayah
import com.example.muslimvn.domain.usecases.SurahDetail
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import com.example.muslimvn.presentation.viewmodels.QuranUiSettings
import com.example.muslimvn.presentation.viewmodels.SurahDetailState
import com.example.muslimvn.presentation.viewmodels.SurahDetailViewModel
import com.example.muslimvn.ui.theme.extendedTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: SurahDetailViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val quranSettings by viewModel.quranSettings.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentMediaId by viewModel.currentMediaId.collectAsState()
    val playingWordIndex by viewModel.playingWordIndex.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Trigger haptic feedback khi word thay đổi
    LaunchedEffect(playingWordIndex) {
        if (playingWordIndex != null && quranSettings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()
    
    var selectedAyah by remember { mutableStateOf<Ayah?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Logic tự động cuộn thông minh (Google Style: Clean & Intelligent)
    var isAutoScrollEnabled by remember { mutableStateOf(true) }

    // Theo dõi tương tác người dùng để tự động bật lại auto-scroll sau 5s im lặng
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            isAutoScrollEnabled = false
        } else {
            // Sau khi ngừng cuộn tay, đợi 5 giây rồi tự động bật lại auto-scroll
            kotlinx.coroutines.delay(5000)
            isAutoScrollEnabled = true
        }
    }

    LaunchedEffect(currentMediaId) {
        if (isAutoScrollEnabled && currentMediaId != null && currentMediaId!!.contains(":")) {
            val parts = currentMediaId!!.split(":")
            val ayahNumber = parts[1].toIntOrNull() ?: 0
            if (state is SurahDetailState.Success) {
                // Cuộn tới item index = ayahNumber (Header là index 0)
                if (ayahNumber < lazyListState.layoutInfo.totalItemsCount) {
                    lazyListState.animateScrollToItem(ayahNumber, scrollOffset = -300)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    if (state is SurahDetailState.Success) {
                        Text(
                            text = (state as SurahDetailState.Success).surahDetail.surah.nameVietnamese,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            progress = { syncProgress },
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (state is SurahDetailState.Success) {
                        IconButton(onClick = { viewModel.playContinuous(1) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all))
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            PodcastPlayerBarState(playerViewModel) { active ->
                androidx.compose.animation.AnimatedVisibility(visible = active != null) {
                    if (active != null) {
                        val quranPlaylist by viewModel.playlist.collectAsState()
                        val podcastPlaylist by playerViewModel.playlist.collectAsState()
                        
                        // Xác định xem đang phát Quran hay Podcast để dùng đúng playlist
                        val isQuran = active.id.contains(":")
                        
                        MiniPlayerBar(
                            title = active.title,
                            subtitle = active.subtitle,
                            artworkPath = active.artworkPath,
                            isPlaying = active.isPlaying,
                            isBuffering = active.isBuffering,
                            positionMs = active.positionMs,
                            durationMs = active.durationMs,
                            speedLabel = com.example.muslimvn.presentation.components.formatSpeedLabel(active.speed),
                            onPlayPauseClick = playerViewModel::togglePlayPause,
                            onSeekTo = playerViewModel::seekTo,
                            onCycleSpeed = playerViewModel::cyclePlaybackSpeed,
                            onOpenFullPlayer = onOpenFullPlayer,
                            currentMediaId = active.id,
                            // Đồng bộ playlist linh hoạt
                            playlist = if (isQuran && state is SurahDetailState.Success) {
                                val sNumber = (state as SurahDetailState.Success).surahDetail.surah.number
                                quranPlaylist.map { ayah ->
                                    com.example.muslimvn.domain.models.PodcastEpisode(
                                        id = "${sNumber}:${ayah.ayahNumber}",
                                        scholarId = sNumber.toString(),
                                        title = "Câu ${ayah.ayahNumber}",
                                        audioUrl = "",
                                        artworkUrl = "icon/quran.png",
                                        duration = 0,
                                        pubDate = 0,
                                        description = "",
                                        isDownloaded = false,
                                        lastPositionMs = 0
                                    )
                                }
                            } else podcastPlaylist,
                            onPlayEpisode = { episode ->
                                if (isQuran) {
                                    val ayahNum = episode.id.split(":").getOrNull(1)?.toIntOrNull() ?: 1
                                    viewModel.playAyah(ayahNum)
                                } else {
                                    playerViewModel.playEpisode(episode)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val currentState = state) {
                is SurahDetailState.Loading -> {
                    LoadingIndicator(
                        label = stringResource(R.string.loading_please_wait),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SurahDetailState.Error -> {
                    ErrorState(
                        message = currentState.message,
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SurahDetailState.Success -> {
                    val surahDetail = currentState.surahDetail
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            SurahHeader(surahDetail.surah.nameArabic, surahDetail.surah.nameVietnamese)
                        }
                        
                        items(surahDetail.ayahs, key = { it.id }) { ayah ->
                            val isAyahPlaying = currentMediaId == "${surahDetail.surah.number}:${ayah.ayahNumber}"
                            val currentPlayingWordIndex = if (isAyahPlaying) playingWordIndex else null
                            
                            AyahItem(
                                ayah = ayah,
                                fontSize = quranSettings.fontSize,
                                displayMode = quranSettings.displayMode,
                                isPlaying = isAyahPlaying && isPlaying,
                                isBuffering = isAyahPlaying && isBuffering,
                                playingWordIndex = currentPlayingWordIndex,
                                onClick = { selectedAyah = ayah }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }

        if (selectedAyah != null) {
            val ayah = selectedAyah!!
            ModalBottomSheet(
                onDismissRequest = { selectedAyah = null },
                sheetState = sheetState
            ) {
                AyahActionsContent(
                    ayah = ayah,
                    isPlaying = currentMediaId == "${(state as SurahDetailState.Success).surahDetail.surah.number}:${ayah.ayahNumber}" && isPlaying,
                    onPlayClick = {
                        viewModel.playAyah(ayah.ayahNumber)
                        selectedAyah = null
                    },
                    onBookmarkClick = {
                        viewModel.toggleBookmark(ayah.id, !ayah.isBookmarked)
                        selectedAyah = null
                    },
                    onShareClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${ayah.textArabic}\n\n${ayah.textVietnamese}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                        selectedAyah = null
                    }
                )
            }
        }
    }
}

@Composable
private fun AyahActionsContent(
    ayah: Ayah,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Câu ${ayah.ayahNumber}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        ListItem(
            headlineContent = { Text(if (isPlaying) "Tạm dừng" else "Phát âm thanh") },
            leadingContent = { 
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null) 
            },
            modifier = Modifier.clickable { onPlayClick() }
        )
        ListItem(
            headlineContent = { Text(if (ayah.isBookmarked) "Bỏ dấu nhớ" else "Đánh dấu câu này") },
            leadingContent = { 
                Icon(if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = null) 
            },
            modifier = Modifier.clickable { onBookmarkClick() }
        )
        ListItem(
            headlineContent = { Text("Chia sẻ câu này") },
            leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
            modifier = Modifier.clickable { onShareClick() }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SurahHeader(nameArabic: String, nameVietnamese: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = nameArabic,
            style = MaterialTheme.extendedTypography.arabicDisplay,
            fontSize = 36.sp, // Tăng từ 30sp lên 36sp cho tiêu đề chính
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = nameVietnamese,
            style = MaterialTheme.typography.titleMedium, // Thay titleLarge bằng titleMedium
            fontWeight = FontWeight.Normal, // Bỏ Bold để giảm sự chú ý
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun AyahItem(
    ayah: Ayah,
    fontSize: Float,
    displayMode: QuranDisplayMode,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playingWordIndex: Int? = null,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ayahPlayingTint"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ayah.ayahNumber.toArabicOrnate(),
                style = MaterialTheme.extendedTypography.arabicInline,
                fontSize = (fontSize * 0.8).sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(4.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (isPlaying) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (ayah.isBookmarked) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (displayMode != QuranDisplayMode.TRANSLATION_ONLY) {
            val words = remember(ayah.textArabic) {
                ayah.textArabic.trim().split(Regex("\\s+"))
            }
            
            val annotatedArabic = buildAnnotatedString {
                words.forEachIndexed { index, word ->
                    val isWordHighlighted = playingWordIndex == index
                    withStyle(
                        style = SpanStyle(
                            color = if (isWordHighlighted) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isWordHighlighted) FontWeight.Bold else FontWeight.Normal,
                            background = if (isWordHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                         else Color.Transparent
                        )
                    ) {
                        append(word)
                    }
                    if (index < words.size - 1) append(" ")
                }
            }

            Text(
                text = annotatedArabic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.extendedTypography.arabicAyah,
                fontSize = (fontSize * 1.4).sp, // To rõ vượt trội
                lineHeight = (fontSize * 2.2).sp // Khoảng cách dòng rộng cho người già dễ đọc
            )
        }

        if (displayMode == QuranDisplayMode.BOTH) {
            Spacer(modifier = Modifier.height(4.dp)) // Thu hẹp khoảng cách với bản dịch
        }

        if (displayMode != QuranDisplayMode.ARABIC_ONLY) {
            Text(
                text = ayah.textVietnamese,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium, // Chữ nhỏ hơn, mảnh hơn
                fontSize = (fontSize * 0.8).sp,
                lineHeight = (fontSize * 1.2).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Mờ hơn để không tranh chấp với tiếng Ả Rập
            )
        }
    }
}

private fun Int.toArabicOrnate(): String = buildString {
    append('﴿')
    this@toArabicOrnate.toString().forEach { ch ->
        if (ch.isDigit()) append('٠' + ch.digitToInt()) else append(ch)
    }
    append('﴾')
}
