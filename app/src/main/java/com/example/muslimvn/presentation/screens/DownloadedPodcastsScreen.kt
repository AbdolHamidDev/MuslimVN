@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

package com.example.muslimvn.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.formatDurationMs
import com.example.muslimvn.presentation.components.formatPubDate
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.screens.scholar.components.LibraryDetailHeader
import com.example.muslimvn.presentation.screens.scholar.components.PodcastSearchBar
import com.example.muslimvn.presentation.screens.scholar.components.rememberDynamicBackgroundColor
import com.example.muslimvn.presentation.viewmodels.DownloadedPodcastItem
import com.example.muslimvn.presentation.viewmodels.DownloadedPodcastsViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import com.example.muslimvn.presentation.viewmodels.formatFileSize

/**
 * Màn hình danh sách các tập Podcast đã tải xuống (Offline Podcast Management):
 * - Thiết kế Edge-to-Edge tràn viền với nút Back tròn nổi bán trong suốt.
 * - Thẻ thống kê tổng dung lượng bộ nhớ đã sử dụng kèm nút "Giải phóng dung lượng".
 * - Dialog xác nhận giải phóng toàn bộ bộ nhớ.
 * - Danh sách chi tiết từng tập podcast offline kèm nút nghe và nút xóa từng tập.
 */
@Composable
fun DownloadedPodcastsScreen(
    onBackClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: DownloadedPodcastsViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val downloadedPodcasts by viewModel.downloadedPodcasts.collectAsStateWithLifecycle()
    val totalSizeBytes by viewModel.totalSizeBytes.collectAsStateWithLifecycle()

    var showClearAllDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<DownloadedPodcastItem?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPodcasts = remember(downloadedPodcasts, searchQuery) {
        if (searchQuery.isBlank()) {
            downloadedPodcasts
        } else {
            downloadedPodcasts.filter {
                it.episode.title.contains(searchQuery, ignoreCase = true) ||
                        it.scholarName.contains(searchQuery, ignoreCase = true) ||
                        it.episode.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val imagePath = "images/library/download.webp"
    val backgroundColor = rememberDynamicBackgroundColor(imagePath)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PodcastPlayerBarState(playerViewModel) { active ->
                val isPodcast = active != null && (!active.id.contains(":") && !active.id.startsWith("islamhouse_"))
                AnimatedVisibility(visible = isPodcast) {
                    if (active != null) {
                        val playlist by playerViewModel.playlist.collectAsStateWithLifecycle()
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
                            playlist = playlist,
                            onPlayEpisode = playerViewModel::playEpisode
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = padding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Immersive Hero Header với ảnh download.webp + Pill Bar 3 Nút UI + Dynamic Background Color
                item(key = "header") {
                    val currentPlayingId by playerViewModel.currentEpisodeId.collectAsStateWithLifecycle()
                    val playerIsPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
                    val isDownloadedPlaying = playerIsPlaying && downloadedPodcasts.any { it.episode.id == currentPlayingId }

                    LibraryDetailHeader(
                        title = "Podcast đã tải xuống",
                        subtitle = "${formatFileSize(totalSizeBytes)} (${downloadedPodcasts.size} tập offline)",
                        imagePath = imagePath,
                        backgroundColor = backgroundColor,
                        isPlaying = isDownloadedPlaying,
                        onPlayAllClick = {
                            if (downloadedPodcasts.isNotEmpty()) {
                                viewModel.playEpisode(downloadedPodcasts[0])
                            }
                        },
                        onShuffleClick = {
                            if (downloadedPodcasts.isNotEmpty()) {
                                viewModel.playEpisode(downloadedPodcasts.shuffled()[0])
                            }
                        },
                        onSearchClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        extraContent = if (downloadedPodcasts.isNotEmpty()) {
                            {
                                Button(
                                    onClick = { showClearAllDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Giải phóng dung lượng",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else null
                    )
                }

                // Thanh tìm kiếm hiển thị khi bấm icon Search trên Pill Header
                item(key = "search_bar") {
                    AnimatedVisibility(visible = isSearchActive) {
                        PodcastSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Tìm kiếm podcast đã tải xuống..."
                        )
                    }
                }

                // Tiêu đề danh sách tập
                if (downloadedPodcasts.isNotEmpty()) {
                    item(key = "section_title") {
                        Text(
                            text = "Danh sách tập podcast offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Trạng thái trống (Chưa có tập nào)
                if (downloadedPodcasts.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyState(
                            message = "Chưa có podcast nào được tải xuống",
                            hint = "Các tập podcast bạn tải offline sẽ xuất hiện tại đây để quản lý"
                        )
                    }
                } else if (filteredPodcasts.isEmpty()) {
                    item(key = "empty_search") {
                        EmptyState(
                            message = "Không tìm thấy kết quả cho \"$searchQuery\"",
                            hint = "Thử tìm kiếm với từ khóa khác"
                        )
                    }
                } else {
                    // Danh sách từng tập podcast offline
                    items(filteredPodcasts, key = { it.episode.id }) { item ->
                        DownloadedPodcastCardItem(
                            item = item,
                            onPlay = { viewModel.playEpisode(item) },
                            onDelete = { itemToDelete = item },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Nút Back tròn bán trong suốt nổi ở góc trên bên trái
            FloatingBackButton(
                onBackClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
            )
        }
    }

    // Dialog xác nhận giải phóng toàn bộ dung lượng Podcast
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "Giải phóng dung lượng Podcast?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa toàn bộ ${downloadedPodcasts.size} tập podcast đã tải về không?\n\nHành động này sẽ giải phóng ${formatFileSize(totalSizeBytes)} bộ nhớ.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllDownloads()
                        showClearAllDialog = false
                    }
                ) {
                    Text(
                        text = "Xóa sạch",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(text = "Hủy", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF18222C)
        )
    }

    // Dialog xác nhận xóa 1 tập podcast đơn lẻ
    if (itemToDelete != null) {
        val targetItem = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Xóa bản tải xuống?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa bản offline của tập:\n\"${targetItem.episode.title}\"?",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEpisode(targetItem)
                        itemToDelete = null
                    }
                ) {
                    Text(
                        text = "Xóa",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(text = "Hủy", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF18222C)
        )
    }
}

/** Card hiển thị 1 tập podcast offline */
@Composable
private fun DownloadedPodcastCardItem(
    item: DownloadedPodcastItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Ảnh bìa tập podcast
            AsyncImage(
                model = (item.episode.artworkUrl ?: "").toAndroidAssetUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Tiêu đề & Học giả & Dung lượng
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.scholarName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(formatFileSize(item.fileSizeBytes))
                        append(" \u2022 ")
                        append(formatDurationMs(item.episode.duration))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút Nghe & Nút Xóa
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Nghe ngay",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa bản tải",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

