package com.example.muslimvn.presentation.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.muslimvn.presentation.viewmodels.TafsirState
import com.example.muslimvn.presentation.viewmodels.TranslationState
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
    val tafsirState by viewModel.tafsirState.collectAsState()
    val translationState by viewModel.translationState.collectAsState()
    val currentTafsirAyah by viewModel.currentTafsirAyah.collectAsState()
    val context = LocalContext.current

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
                        val playlist by playerViewModel.playlist.collectAsState()
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
                            playlist = if (isQuran && state is SurahDetailState.Success) {
                                val sNumber = (state as SurahDetailState.Success).surahDetail.surah.number
                                (state as SurahDetailState.Success).surahDetail.ayahs.map { ayah ->
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
                            } else playlist,
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
                        
                        if (surahDetail.surah.number != 1 && surahDetail.surah.number != 9) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        style = MaterialTheme.extendedTypography.arabicHeading,
                                        fontSize = (quranSettings.fontSize * 1.2).sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
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
                                isAnyAyahPlaying = isPlaying && currentMediaId != null,
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
                    },
                    onTafsirClick = {
                        val currentAyah = selectedAyah // Lưu lại để dùng sau khi menu đóng
                        selectedAyah = null // Đóng menu hành động ngay lập tức
                        if (currentAyah != null) {
                            viewModel.loadTafsir(currentAyah)
                        }
                    }
                )
            }
        }

        if (tafsirState !is TafsirState.Idle) {
            TafsirBottomSheet(
                state = tafsirState,
                translationState = translationState,
                ayah = currentTafsirAyah,
                onTranslateClick = viewModel::translateCurrentTafsir,
                onDismiss = { viewModel.clearTafsir() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TafsirBottomSheet(
    state: TafsirState,
    translationState: TranslationState,
    ayah: Ayah?,
    onTranslateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var showTranslation by remember { mutableStateOf(false) }
    
    // Tự động bật hiển thị tiếng Việt nếu dịch thành công
    LaunchedEffect(translationState) {
        if (translationState is TranslationState.Success) {
            showTranslation = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxSize(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header: Title & Translation Toggle (AssistChip style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tafsir Ibn Kathir",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (state is TafsirState.Success) {
                    val tafsir = state.tafsir
                    val hasTranslation = tafsir.translatedText != null
                    
                    AssistChip(
                        onClick = { 
                            if (hasTranslation) {
                                showTranslation = !showTranslation 
                            } else {
                                onTranslateClick()
                            }
                        },
                        label = { 
                            val label = when {
                                hasTranslation && showTranslation -> "Xem bản gốc (EN)"
                                hasTranslation && !showTranslation -> "Xem tiếng Việt"
                                translationState is TranslationState.DownloadingModel -> "Đang tải model..."
                                translationState is TranslationState.Translating -> "Đang dịch..."
                                else -> "Dịch sang VI"
                            }
                            Text(label, style = MaterialTheme.typography.labelMedium) 
                        },
                        leadingIcon = {
                            if (translationState is TranslationState.DownloadingModel || translationState is TranslationState.Translating) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        shape = CircleShape,
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Ayah Context: Hiển thị như một "Reference Card" của Google
                if (ayah != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "VERSE ${ayah.surahId}:${ayah.ayahNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = ayah.textArabic,
                                        style = MaterialTheme.extendedTypography.arabicAyah,
                                        fontSize = 24.sp,
                                        lineHeight = 40.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                when (state) {
                    is TafsirState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        }
                    }
                    is TafsirState.Error -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.message,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is TafsirState.Success -> {
                        val tafsirData = state.tafsir
                        item {
                            if (showTranslation && tafsirData.translatedText != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(bottom = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Bản dịch ngoại tuyến bởi Google AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                            
                            val textToDisplay = if (showTranslation && tafsirData.translatedText != null) {
                                tafsirData.translatedText
                            } else {
                                tafsirData.text
                            }

                            val cleanText = remember(textToDisplay) {
                                textToDisplay
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("&nbsp;", " ")
                                    .replace("&quot;", "\"")
                                    .trim()
                            }
                            
                            Text(
                                text = cleanText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 17.sp,
                                lineHeight = 30.sp,
                                textAlign = TextAlign.Justify,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                    else -> {}
                }
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
    onShareClick: () -> Unit,
    onTafsirClick: () -> Unit
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
            headlineContent = { Text("Xem giải thích (Tafsir)") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            modifier = Modifier.clickable { onTafsirClick() }
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
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.onSurface
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
    isAnyAyahPlaying: Boolean = false,
    onClick: () -> Unit
) {
    // Focus Effect: Chỉ mờ khi CÓ audio đang phát toàn cục. Nếu không phát gì, tất cả đều rõ nét (Alpha 1.0)
    val itemAlpha by animateFloatAsState(
        targetValue = if (isAnyAyahPlaying && !isPlaying) 0.4f else 1.0f,
        animationSpec = tween(600),
        label = "itemAlpha"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ayahPlayingTint"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 16.dp)
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
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else if (isPlaying) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            if (ayah.isBookmarked) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (displayMode != QuranDisplayMode.TRANSLATION_ONLY) {
            val words = remember(ayah.textArabic) {
                ayah.textArabic.trim().split(Regex("\\s+"))
            }
            
            val waqfMarks = remember { 
                setOf("ۖ", "ۗ", "ۚ", "ۛ", "ۜ", "ۘ", "ۙ", "ۣ", "۞", "۝") 
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp) // ⚡ TĂNG PADDING: Đảm bảo các dấu harakat không bao giờ bị đè nhau
                ) {
                    var logicalWordIndex = 0
                    words.forEach { word ->
                        val isWaqfMark = waqfMarks.contains(word) || 
                                         (word.length == 1 && word[0] in '\u06D6'..'\u06DC')
                        
                        val isHighlighted = !isWaqfMark && playingWordIndex == logicalWordIndex
                        
                        WordItem(
                            word = word,
                            fontSize = (fontSize * 1.4).sp,
                            isHighlighted = isHighlighted,
                            isWaqfMark = isWaqfMark,
                            isAnyAyahPlaying = isAnyAyahPlaying
                        )
                        
                        if (!isWaqfMark) logicalWordIndex++
                    }
                }
            }
        }

        if (displayMode == QuranDisplayMode.BOTH) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (displayMode != QuranDisplayMode.ARABIC_ONLY) {
            Text(
                text = ayah.textVietnamese,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = (fontSize * 0.85).sp,
                lineHeight = (fontSize * 1.3).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun WordItem(
    word: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isHighlighted: Boolean,
    isWaqfMark: Boolean,
    isAnyAyahPlaying: Boolean
) {
    // Hoạt ảnh cực kỳ nhẹ nhàng, trang trọng (Dùng tween thay vì spring để không nhún nhảy)
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1.0f,
        animationSpec = tween(400, easing = LinearOutSlowInEasing),
        label = "wordScale"
    )
    
    // Chỉ làm mờ các chữ khác khi CÓ audio đang phát toàn cục.
    // Tăng độ mờ lên 0.5 để người dùng vẫn có thể đọc được các chữ xung quanh dễ dàng.
    val alpha by animateFloatAsState(
        targetValue = if (isHighlighted) 1.0f 
                     else if (isAnyAyahPlaying) (if (isWaqfMark) 0.5f else 0.45f) 
                     else 1.0f,
        animationSpec = tween(500),
        label = "wordAlpha"
    )
    
    val color by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary 
                     else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(500),
        label = "wordColor"
    )

    Text(
        text = word,
        fontSize = fontSize,
        fontFamily = com.example.muslimvn.ui.theme.ArabicFontFamily,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        color = color,
        modifier = Modifier
            .padding(horizontal = 3.dp) // Tăng nhẹ khoảng cách ngang giữa các từ
            .alpha(alpha)
            .scale(scale)
    )
}

private fun Int.toArabicOrnate(): String = buildString {
    append('﴿')
    this@toArabicOrnate.toString().forEach { ch ->
        if (ch.isDigit()) append('٠' + ch.digitToInt()) else append(ch)
    }
    append('﴾')
}
