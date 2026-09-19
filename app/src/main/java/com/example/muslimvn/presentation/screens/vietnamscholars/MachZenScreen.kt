package com.example.muslimvn.presentation.screens.vietnamscholars

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muslimvn.ui.theme.CategoryColors
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.ScholarDocument
import com.example.muslimvn.domain.models.YoutubeVideo
import java.util.Locale
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun MachZenScreen(
    onBackClick: () -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onDocumentClick: (String, String) -> Unit,
    onOpenFullPlayer: () -> Unit,
    viewModel: MachZenViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shimmerBrush = rememberMachZenShimmerBrush()
    val listState = rememberLazyListState()

    var isSearchActive by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemCount = listState.layoutInfo.totalItemsCount
            lastVisibleItemIndex >= totalItemCount - 3 && !uiState.isLoadingMore && uiState.hasNextPage
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMoreVideos()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = globalSearchQuery,
                            onValueChange = { globalSearchQuery = it },
                            placeholder = { Text("Tìm kiếm bài giảng, tài liệu...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("Học giả Mách Zên")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            globalSearchQuery = ""
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) globalSearchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Đóng tìm kiếm" else "Tìm kiếm"
                        )
                    }
                    if (uiState.selectedTab == 0 && !isSearchActive) {
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (uiState.isGridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewAgenda,
                                contentDescription = "Chuyển đổi chế độ hiển thị"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    // Tab Video
                    if (uiState.isLoading) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Bài giảng video",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(6) {
                                if (uiState.isGridMode) {
                                    MachZenCardItemShimmer(shimmerBrush)
                                } else {
                                    MachZenItemShimmer(shimmerBrush)
                                }
                            }
                        }
                    } else if (uiState.error != null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Lỗi: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 180.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Bài giảng video",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (uiState.videos.isEmpty()) {
                                item {
                                    Text(
                                        text = "Chưa có video nào hoặc không thể tải dữ liệu.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(uiState.videos) { video ->
                                    if (uiState.isGridMode) {
                                        MachZenVideoCardItem(
                                            video = video,
                                            onClick = { onVideoClick(video.videoUrl, video.title, "", "https://www.youtube.com/@islamlavn/videos") }
                                        )
                                    } else {
                                        MachZenVideoListItem(
                                            video = video,
                                            onClick = { onVideoClick(video.videoUrl, video.title, "", "https://www.youtube.com/@islamlavn/videos") }
                                        )
                                    }
                                }

                                if (uiState.isLoadingMore) {
                                    items(2) {
                                        if (uiState.isGridMode) {
                                            MachZenCardItemShimmer(shimmerBrush)
                                        } else {
                                            MachZenItemShimmer(shimmerBrush)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Tab Âm thanh (Bài giảng âm thanh - MP3)
                    val audioListState = rememberLazyListState()
                    val shouldLoadMoreAudios = remember {
                        derivedStateOf {
                            val lastVisibleItemIndex = audioListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItemCount = audioListState.layoutInfo.totalItemsCount
                            lastVisibleItemIndex >= totalItemCount - 3 && !uiState.isLoadingMoreDocuments && uiState.documentHasNextPage
                        }
                    }

                    LaunchedEffect(shouldLoadMoreAudios.value) {
                        if (shouldLoadMoreAudios.value) {
                            viewModel.loadMoreDocuments()
                        }
                    }

                    val displayedAudios = remember(uiState.documents, globalSearchQuery) {
                        val baseList = uiState.documents.filter { it.fileExtension?.uppercase() == "MP3" }
                        if (globalSearchQuery.isBlank()) {
                            baseList
                        } else {
                            baseList.filter {
                                it.title.contains(globalSearchQuery, ignoreCase = true) ||
                                        (it.description?.contains(globalSearchQuery, ignoreCase = true) == true)
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isLoadingDocuments) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Đang tải bài giảng âm thanh...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(5) {
                                    MachZenDocumentItemShimmer(shimmerBrush)
                                }
                            }
                        } else if (uiState.documentError != null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Lỗi: ${uiState.documentError}", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            LazyColumn(
                                state = audioListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = if (globalSearchQuery.isNotEmpty()) "Kết quả tìm kiếm cho: \"$globalSearchQuery\"" else "Bài giảng âm thanh & Podcast (MP3)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (displayedAudios.isEmpty()) {
                                    if (globalSearchQuery.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Không tìm thấy âm thanh phù hợp.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        items(5) {
                                            MachZenDocumentItemShimmer(shimmerBrush)
                                        }
                                    }
                                } else {
                                    items(displayedAudios) { doc ->
                                        MachZenDocumentItemCard(
                                            document = doc,
                                            onClick = {
                                                viewModel.playAudioDocument(doc)
                                            }
                                        )
                                    }

                                    if (uiState.isLoadingMoreDocuments) {
                                        items(2) {
                                            MachZenDocumentItemShimmer(shimmerBrush)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Tab Tài liệu (Sách & Tài liệu nghiên cứu dạng chữ - PDF, DOCX)
                    val docListState = rememberLazyListState()
                    val shouldLoadMoreDocs = remember {
                        derivedStateOf {
                            val lastVisibleItemIndex = docListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItemCount = docListState.layoutInfo.totalItemsCount
                            lastVisibleItemIndex >= totalItemCount - 3 && !uiState.isLoadingMoreDocuments && uiState.documentHasNextPage
                        }
                    }

                    LaunchedEffect(shouldLoadMoreDocs.value) {
                        if (shouldLoadMoreDocs.value) {
                            viewModel.loadMoreDocuments()
                        }
                    }

                    val displayedDocs = remember(uiState.filteredDocuments, globalSearchQuery) {
                        val baseList = uiState.filteredDocuments.filter { it.fileExtension?.uppercase() != "MP3" }
                        if (globalSearchQuery.isBlank()) {
                            baseList
                        } else {
                            baseList.filter {
                                it.title.contains(globalSearchQuery, ignoreCase = true) ||
                                        (it.description?.contains(globalSearchQuery, ignoreCase = true) == true)
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Document Type Filter Chips (Chỉ hiển thị định dạng chữ)
                        val types = listOf("TẤT CẢ", "PDF", "DOCX")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(types) { type ->
                                val chipIcon = when (type) {
                                    "PDF" -> Icons.Default.Description
                                    "DOCX" -> Icons.Default.Description
                                    else -> Icons.Default.Folder
                                }
                                FilterChip(
                                    selected = uiState.selectedDocType == type,
                                    onClick = { viewModel.setDocumentType(type) },
                                    label = { Text(type) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = chipIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        if (uiState.isLoadingDocuments) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Đang tải danh sách tài liệu...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(5) {
                                    MachZenDocumentItemShimmer(shimmerBrush)
                                }
                            }
                        } else if (uiState.documentError != null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Lỗi: ${uiState.documentError}", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            LazyColumn(
                                state = docListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = if (globalSearchQuery.isNotEmpty()) "Kết quả tìm kiếm cho: \"$globalSearchQuery\"" else "Tài liệu & Sách nghiên cứu văn bản (${uiState.selectedDocType})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (displayedDocs.isEmpty()) {
                                    if (globalSearchQuery.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Không tìm thấy tài liệu phù hợp.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        items(5) {
                                            MachZenDocumentItemShimmer(shimmerBrush)
                                        }
                                    }
                                } else {
                                    items(displayedDocs) { doc ->
                                        MachZenDocumentItemCard(
                                            document = doc,
                                            onClick = {
                                                val url = doc.downloadUrl ?: doc.detailUrl ?: "https://islamhouse.com/vi/author/193689/showall/vi/1/"
                                                onDocumentClick(url, doc.title)
                                            }
                                        )
                                    }

                                    if (uiState.isLoadingMoreDocuments && uiState.selectedDocType == "TẤT CẢ") {
                                        items(2) {
                                            MachZenDocumentItemShimmer(shimmerBrush)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Frosted Glass / Translucent Pill Bar at bottom center
            MachZenFloatingPillBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // Audio Mini Player logic - CHỈ hiển thị âm thanh riêng của Mách Zên (islamhouse_)
            PodcastPlayerBarState(playerViewModel) { active ->
                val isIslamHouse = active != null && active.id.startsWith("islamhouse_")
                if (isIslamHouse && active != null) {
                    val currentPlaylist = uiState.documents
                        .filter { it.fileExtension?.equals("MP3", ignoreCase = true) == true && !it.downloadUrl.isNullOrBlank() }
                        .map { doc ->
                            PodcastEpisode(
                                id = "islamhouse_${doc.id}",
                                scholarId = "mach_zen",
                                title = doc.title,
                                audioUrl = doc.downloadUrl ?: "",
                                artworkUrl = "images/featured_scholars_vietnam/mach_zen.webp",
                                duration = 0L,
                                pubDate = doc.addDate?.times(1000) ?: 0L,
                                description = doc.description ?: "",
                                isDownloaded = false,
                                lastPositionMs = 0L
                            )
                        }

                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
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
                            playlist = currentPlaylist,
                            onPlayEpisode = { ep ->
                                val doc = uiState.documents.find { "islamhouse_${it.id}" == ep.id }
                                if (doc != null) viewModel.playAudioDocument(doc)
                            }
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun MachZenDocumentItemCard(
    document: ScholarDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileExt = document.fileExtension?.uppercase() ?: ""
    val fileColors = CategoryColors.getDocumentFileColors(fileExt)
    val containerColor = fileColors.containerColor
    val iconColor = fileColors.iconColor

    val hasCoverPattern = remember(document.title) {
        val topicColors = CategoryColors.getScholarTopicColors(document.id)
        Triple(topicColors.primaryColor, topicColors.secondaryColor, topicColors.title)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fileExt == "PDF" || fileExt == "DOCX") {
                Card(
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .width(62.dp)
                        .height(86.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(hasCoverPattern.first, hasCoverPattern.first.copy(alpha = 0.85f))
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(5.dp)
                                .background(Color.Black.copy(alpha = 0.15f))
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = document.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = hasCoverPattern.second,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 10.sp
                            )
                            
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = fileExt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!document.description.isNullOrBlank()) {
                    Text(
                        text = document.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fileExt.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(fileExt, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    if (!document.fileSize.isNullOrBlank()) {
                        Text(
                            text = document.fileSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MachZenVideoListItem(
    video: YoutubeVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        val hours = video.duration / 3600
                        val minutes = (video.duration % 3600) / 60
                        val seconds = video.duration % 60
                        val durationText = if (hours > 0) {
                            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format(Locale.US, "%d:%02d", minutes, seconds)
                        }
                        Text(
                            text = durationText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = video.uploadDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MachZenVideoCardItem(
    video: YoutubeVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16/9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        val hours = video.duration / 3600
                        val minutes = (video.duration % 3600) / 60
                        val seconds = video.duration % 60
                        val durationText = if (hours > 0) {
                            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format(Locale.US, "%d:%02d", minutes, seconds)
                        }
                        Text(
                            text = durationText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = video.uploadDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MachZenFloatingPillBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillTabItem(
                title = "Video",
                icon = Icons.Default.VideoLibrary,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            PillTabItem(
                title = "Âm thanh",
                icon = Icons.Default.MusicNote,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            PillTabItem(
                title = "Tài liệu",
                icon = Icons.AutoMirrored.Filled.Article,
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
fun PillTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "bg_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "content_color"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun rememberMachZenShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "mach_zen_shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(
        baseColor.copy(alpha = 0.3f),
        baseColor.copy(alpha = 0.7f),
        baseColor.copy(alpha = 0.3f)
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(translateAnim.value, translateAnim.value)
    )
}

@Composable
fun MachZenItemShimmer(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun MachZenCardItemShimmer(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(16/9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(brush)
            )
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun MachZenDocumentItemShimmer(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}
