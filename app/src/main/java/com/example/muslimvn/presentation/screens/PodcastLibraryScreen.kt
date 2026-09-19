@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

package com.example.muslimvn.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.muslimvn.ui.theme.extendedColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.muslimvn.data.util.PodcastDownloadState
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.formatDurationMs
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.screens.scholar.components.EpisodeMoreMenuBottomSheet
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.viewmodels.FavoriteEpisodeItem
import com.example.muslimvn.presentation.viewmodels.PodcastLibraryViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Màn hình Thư viện Podcast (Podcast Library Screen mới):
 * - Header: Nút Back + Title "Thư viện" góc trái.
 * - Grid 2x2 chứa 4 Card chữ nhật: Yêu thích, Danh sách phát, Đã tải xuống, Đã theo dõi.
 * - Mục "Lịch sử nghe" hiển thị các tập podcast vừa nghe gần đây (Recently Played).
 */
@Composable
fun PodcastLibraryScreen(
    onBackClick: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToPlaylist: () -> Unit = {},
    onNavigateToDownloadedPodcasts: () -> Unit = {},
    onNavigateToFollowed: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: PodcastLibraryViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val favoriteEpisodes by viewModel.favoriteEpisodes.collectAsStateWithLifecycle()
    val playlistEpisodes by viewModel.playlistEpisodes.collectAsStateWithLifecycle()
    val recentlyPlayedEpisodes by viewModel.recentlyPlayedEpisodes.collectAsStateWithLifecycle()
    val scholars by viewModel.scholars.collectAsStateWithLifecycle()
    val favoriteEpisodeIds by playerViewModel.favoriteEpisodeIds.collectAsStateWithLifecycle()
    val playlistEpisodeIds by viewModel.playlistEpisodeIds.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var selectedEpisodeForMenu by remember { mutableStateOf<PodcastEpisode?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PodcastPlayerBarState(playerViewModel) { active ->
                val isPodcast = active != null && (!active.id.contains(":") && !active.id.startsWith("islamhouse_"))
                AnimatedVisibility(visible = isPodcast) {
                    if (active != null) {
                        val playlist by playerViewModel.playlist.collectAsStateWithLifecycle()
                        val activeIsFav = active.id in favoriteEpisodeIds
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
                            onPlayEpisode = playerViewModel::playEpisode,
                            isFavorite = activeIsFav,
                            onToggleFavorite = { playerViewModel.toggleFavorite(active.id) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding()),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // 1. Top Header: Nút Back + Tiêu đề "Thư viện" góc trái
                item(key = "top_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        FloatingBackButton(onBackClick = onBackClick)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Thư viện",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Grid 2x2 chứa 4 Card hình chữ nhật
                item(key = "cards_grid") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LibraryGridCard(
                                title = "Yêu thích",
                                subtitle = "${favoriteEpisodes.size} tập",
                                icon = Icons.Default.Favorite,
                                iconTint = MaterialTheme.colorScheme.error,
                                onClick = onNavigateToFavorites,
                                modifier = Modifier.weight(1f)
                            )
                            LibraryGridCard(
                                title = "Danh sách phát",
                                subtitle = "${playlistEpisodes.size} tập",
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                iconTint = MaterialTheme.extendedColors.warning,
                                onClick = onNavigateToPlaylist,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LibraryGridCard(
                                title = "Đã tải xuống",
                                subtitle = "Bản nghe offline",
                                icon = Icons.Default.DownloadForOffline,
                                iconTint = MaterialTheme.extendedColors.success,
                                onClick = onNavigateToDownloadedPodcasts,
                                modifier = Modifier.weight(1f)
                            )
                            LibraryGridCard(
                                title = "Đã theo dõi",
                                subtitle = "${scholars.size} học giả",
                                icon = Icons.Default.Bookmarks,
                                iconTint = MaterialTheme.extendedColors.info,
                                onClick = onNavigateToFollowed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Tiêu đề "Lịch sử nghe" & Danh sách Lịch sử / Empty State
                item(key = "listening_history_section_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lịch sử nghe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                if (recentlyPlayedEpisodes.isEmpty()) {
                    item(key = "listening_history_empty") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyState(
                                message = "Chưa có lịch sử nghe gần đây",
                                hint = "Các tập podcast bạn vừa nghe sẽ xuất hiện tại đây"
                            )
                        }
                    }
                } else {
                    items(recentlyPlayedEpisodes, key = { "history_${it.episode.id}" }) { item ->
                        val currentEpisodeId by playerViewModel.currentEpisodeId.collectAsStateWithLifecycle()
                        val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

                        RecentlyPlayedRow(
                            item = item,
                            isCurrent = currentEpisodeId == item.episode.id,
                            isPlaying = isPlaying,
                            onPlay = { viewModel.playEpisode(item.episode, item.scholarName) },
                            onMoreClick = { selectedEpisodeForMenu = item.episode },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
}

            // BottomSheet khi chọn menu 3 chấm của 1 tập trong Lịch sử nghe
            if (selectedEpisodeForMenu != null) {
                val selectedEp = selectedEpisodeForMenu!!
                val selectedEpDownloadState = downloadStates[selectedEp.id]
                    ?: if (selectedEp.isDownloaded) PodcastDownloadState.Downloaded else PodcastDownloadState.Idle
                val isFav = selectedEp.id in favoriteEpisodeIds
                val inPlaylist = selectedEp.id in playlistEpisodeIds

                EpisodeMoreMenuBottomSheet(
                    episode = selectedEp,
                    scholar = null,
                    onDismiss = { selectedEpisodeForMenu = null },
                    downloadState = selectedEpDownloadState,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(selectedEp.id) },
                    isInPlaylist = inPlaylist,
                    onTogglePlaylist = { viewModel.togglePlaylist(selectedEp.id) },
                    onDownloadClick = { viewModel.downloadEpisode(selectedEp) },
                    onCancelDownloadClick = { viewModel.cancelDownload(selectedEp.id) },
                    onDeleteDownloadClick = { viewModel.deleteDownloadedEpisode(selectedEp) }
                )
            }
        }
    }
}

/** Dòng hiển thị 1 tập podcast trong Lịch sử nghe (Recently Played) */
@Composable
private fun RecentlyPlayedRow(
    item: FavoriteEpisodeItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onPlay),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Artwork
            AsyncImage(
                model = (item.episode.artworkUrl ?: "").toAndroidAssetUri(),
                contentDescription = item.episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.scholarName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                val duration = item.episode.duration
                val pos = item.episode.lastPositionMs
                val isFinished = item.episode.playCount > 0 && (pos == 0L || (duration > 0 && pos >= duration - 15_000L))

                if (isCurrent && isPlaying) {
                    Text(
                        text = "Đang phát...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isFinished) {
                    Text(
                        text = "Đã nghe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else if (pos > 0 && duration > 0) {
                    val progressRatio = (pos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${formatDurationMs(pos)} / ${formatDurationMs(duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (duration > 0) {
                    Text(
                        text = formatDurationMs(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Tùy chọn",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Card hình chữ nhật trong lưới 2x2 của Thư viện */
@Composable
private fun LibraryGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .bouncyClick(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.18f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

