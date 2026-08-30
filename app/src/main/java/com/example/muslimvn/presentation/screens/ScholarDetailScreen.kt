package com.example.muslimvn.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.formatDurationMs
import com.example.muslimvn.presentation.components.formatPubDate
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import com.example.muslimvn.presentation.viewmodels.ScholarDetailViewModel

/**
 * Màn chi tiết học giả: header avatar/bio/tổng số tập + danh sách tập phát (RSS cache)
 * với nút phát/tạm dừng từng tập và chỉ báo "phát tiếp từ vị trí đã lưu".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarDetailScreen(
    onBackClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: ScholarDetailViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val episodes = viewModel.episodesPagingData.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.scholar?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            PodcastPlayerBarState(playerViewModel) { active ->
                val isQuran = active?.id?.contains(":") == true
                AnimatedVisibility(visible = active != null && !isQuran) {
                    if (active != null) {
                        val playlist by playerViewModel.playlist.collectAsState()
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
        when {
            state.isLoading -> LoadingIndicator(
                label = stringResource(R.string.loading_please_wait),
                modifier = Modifier.fillMaxSize()
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "header") {
                    ScholarHeader(scholar = state.scholar, episodeCount = state.totalEpisodesCount)
                }
                if (state.isRefreshing) {
                    item(key = "refreshing") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                if (state.offlineError) {
                    item(key = "offline_banner") {
                        OfflineBanner(onRetry = viewModel::refreshEpisodes)
                    }
                }
                item(key = "episodes_title") {
                    Text(
                        text = stringResource(R.string.podcast_episodes_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                // Loading state cho trang đầu tiên của Paging
                if (episodes.loadState.refresh is LoadState.Loading && state.totalEpisodesCount == 0) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (episodes.itemCount == 0 && !state.isLoading && !state.isRefreshing && episodes.loadState.refresh is LoadState.NotLoading) {
                    item(key = "empty") {
                        EmptyState(
                            message = stringResource(R.string.podcast_empty_episodes),
                            hint = stringResource(R.string.podcast_empty_episodes_hint)
                        )
                    }
                }
                
                items(
                    count = episodes.itemCount,
                    key = { index -> episodes[index]?.id ?: "placeholder_$index" }
                ) { index ->
                    val episode = episodes[index]
                    if (episode != null) {
                        EpisodeRow(
                            episode = episode,
                            scholarName = state.scholar?.name,
                            isCurrent = viewModel.currentEpisodeId.collectAsState().value == episode.id,
                            isPlaying = viewModel.isPlaying.collectAsState().value,
                            isBuffering = viewModel.isBuffering.collectAsState().value,
                            onPlayPause = { viewModel.onPlayPauseClicked(episode, state.scholar?.name) }
                        )
                    }
                }

                // Loading state cho tải thêm trang (Append)
                if (episodes.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Header học giả: avatar lớn, tên, chức danh, bio và tổng số tập đã cache. */
@Composable
private fun ScholarHeader(scholar: Scholar?, episodeCount: Int) {
    if (scholar == null) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            AsyncImage(
                model = scholar.avatarPath.toAndroidAssetUri(),
                contentDescription = scholar.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = scholar.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = scholar.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = stringResource(R.string.podcast_total_episodes, episodeCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            if (scholar.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = scholar.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Banner nhỏ báo lỗi fetch RSS (offline/feed lỗi) kèm nút thử lại — cache cũ vẫn dùng được. */
@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.podcast_error_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

/**
 * Một dòng tập phát: tiêu đề, ngày phát • thời lượng, badge "phát tiếp tục" nếu còn
 * vị trí lưu, và nút phát/tạm dừng tròn bên phải. Hàng đang phát được tô nền nhạt.
 */
@Composable
private fun EpisodeRow(
    episode: PodcastEpisode,
    scholarName: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit
) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .bouncyClick(onClick = onPlayPause)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(formatPubDate(episode.pubDate))
                            append(" \u2022 ")
                            append(formatDurationMs(episode.duration))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Còn vị trí nghe hợp lệ -> gợi ý phát tiếp từ đó.
                    val resumable = episode.lastPositionMs > 60_000L &&
                        (episode.duration <= 0 || episode.lastPositionMs < episode.duration - 15_000L)
                    if (resumable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.podcast_resume_from,
                                formatDurationMs(episode.lastPositionMs)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            FilledIconButton(
                onClick = onPlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.size(42.dp)
            ) {
                if (isCurrent && isBuffering) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(if (isCurrent && isPlaying) R.string.pause else R.string.play),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
