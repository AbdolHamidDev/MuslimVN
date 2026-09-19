@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

package com.example.muslimvn.presentation.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.muslimvn.R
import com.example.muslimvn.data.util.PodcastDownloadState
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.screens.scholar.components.EpisodeMoreMenuBottomSheet
import com.example.muslimvn.presentation.screens.scholar.components.EpisodeRow
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.screens.scholar.components.FloatingLibraryButton
import com.example.muslimvn.presentation.screens.scholar.components.OfflineBanner
import com.example.muslimvn.presentation.screens.scholar.components.ScholarHeader
import com.example.muslimvn.presentation.screens.scholar.components.toDeepDark
import com.example.muslimvn.presentation.screens.scholar.components.toMiniPlayerColor
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import com.example.muslimvn.presentation.viewmodels.ScholarDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Màn chi tiết học giả phong cách Dynamic Color YouTube Music chuẩn Material 3.
 * Đã tách modular các subcomponents vào package [com.example.muslimvn.presentation.screens.scholar.components]
 * giúp dễ dàng bảo trì và bổ sung logic tính năng trong tương lai.
 */
@Composable
fun ScholarDetailScreen(
    onBackClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: ScholarDetailViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentEpisodeId by viewModel.currentEpisodeId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val episodes = viewModel.episodesPagingData.collectAsLazyPagingItems()
    val scholarEpisodes by viewModel.scholarEpisodes.collectAsStateWithLifecycle()

    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val playlistProgresses by viewModel.playlistProgresses.collectAsStateWithLifecycle()
    val scholarPlaylistProgress = playlistProgresses[viewModel.scholarId]
    val favoriteEpisodeIds by playerViewModel.favoriteEpisodeIds.collectAsStateWithLifecycle()
    val playlistEpisodeIds by viewModel.playlistEpisodeIds.collectAsStateWithLifecycle()

    val isPlayingScholar = remember(isPlaying, currentEpisodeId, scholarEpisodes) {
        isPlaying && scholarEpisodes.any { it.id == currentEpisodeId }
    }

    // Trạng thái hiển thị BottomSheet tùy chọn cho tập podcast được chọn
    var selectedEpisodeForMenu by remember { mutableStateOf<PodcastEpisode?>(null) }

    // Trích xuất màu đậm tối Material 3 từ ảnh học giả chuẩn YouTube Music
    val context = LocalContext.current
    var dynamicBackgroundColor by remember { mutableStateOf<Color?>(null) }
    val scholarAvatarUri = state.scholar?.avatarPath?.toAndroidAssetUri()

    LaunchedEffect(scholarAvatarUri) {
        if (!scholarAvatarUri.isNullOrEmpty()) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(scholarAvatarUri)
                    .allowHardware(false)
                    .size(200, 200)
                    .build()

                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap).generate()
                        }
                        val swatch = palette.darkVibrantSwatch
                            ?: palette.darkMutedSwatch
                            ?: palette.dominantSwatch
                        swatch?.let {
                            dynamicBackgroundColor = Color(it.rgb).toDeepDark()
                        }
                    }
                }
            }
        }
    }

    val defaultBg = MaterialTheme.colorScheme.surface
    val backgroundColor by animateColorAsState(
        targetValue = dynamicBackgroundColor ?: defaultBg,
        animationSpec = tween(durationMillis = 600),
        label = "scholarDynamicBackground"
    )

    val miniPlayerColor = remember(backgroundColor) {
        backgroundColor.toMiniPlayerColor()
    }

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
                            onToggleFavorite = { playerViewModel.toggleFavorite(active.id) },
                            containerColor = miniPlayerColor
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
            when {
                state.isLoading -> LoadingIndicator(
                    label = stringResource(R.string.loading_please_wait),
                    color = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = padding.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "header") {
                        ScholarHeader(
                            scholar = state.scholar,
                            backgroundColor = backgroundColor,
                            isPlayingScholar = isPlayingScholar,
                            playlistProgress = scholarPlaylistProgress,
                            onPlayAllClick = viewModel::onPlayAllClicked,
                            onShuffleClick = viewModel::onShuffleClicked,
                            onDownloadAllClick = viewModel::downloadScholarPlaylist
                        )
                    }
                    if (state.isRefreshing) {
                        item(key = "refreshing") {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                    if (state.offlineError) {
                        item(key = "offline_banner") {
                            OfflineBanner(onRetry = viewModel::refreshEpisodes)
                        }
                    }
                    item(key = "episodes_title") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.podcast_episodes_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (state.totalEpisodesCount > 0) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.podcast_total_episodes, state.totalEpisodesCount),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Loading state cho trang đầu tiên của Paging
                    if (episodes.loadState.refresh is LoadState.Loading && state.totalEpisodesCount == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    if (episodes.itemCount == 0 && !state.isRefreshing && episodes.loadState.refresh is LoadState.NotLoading) {
                        item(key = "empty") {
                            EmptyState(
                                message = stringResource(R.string.podcast_empty_episodes),
                                hint = stringResource(R.string.podcast_empty_episodes_hint)
                            )
                        }
                    }

                    items(
                        count = episodes.itemCount,
                        key = { index -> episodes[index]?.id ?: "placeholder_$index" },
                        contentType = { "episode" }
                    ) { index ->
                        val episode = episodes[index]
                        if (episode != null) {
                            val epDownloadState = downloadStates[episode.id]
                                ?: if (episode.isDownloaded) PodcastDownloadState.Downloaded else PodcastDownloadState.Idle
                            EpisodeRow(
                                episode = episode,
                                isCurrent = currentEpisodeId == episode.id,
                                modifier = Modifier.animateItem(),
                                isPlaying = isPlaying,
                                downloadState = epDownloadState,
                                onPlay = { viewModel.onPlayPauseClicked(episode, state.scholar?.name) },
                                onMoreClick = { selectedEpisodeForMenu = episode }
                            )
                        }
                    }

                    // Loading state cho tải thêm trang (Append)
                    if (episodes.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White)
                            }
                        }
                    }
                }
            }

            // Floating Back Button ở góc trên bên trái đè lên ảnh nền, có padding an toàn Safe Area / Status Bar
            FloatingBackButton(
                onBackClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
            )

            // Floating Library Button ở góc trên bên phải
            FloatingLibraryButton(
                onLibraryClick = onLibraryClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 12.dp)
            )

            // BottomSheet hiển thị khi nhấn nút 3 chấm của một tập podcast
            if (selectedEpisodeForMenu != null) {
                val selectedEp = selectedEpisodeForMenu!!
                val selectedEpDownloadState = downloadStates[selectedEp.id]
                    ?: if (selectedEp.isDownloaded) PodcastDownloadState.Downloaded else PodcastDownloadState.Idle
                val selectedEpIsFav = selectedEp.id in favoriteEpisodeIds
                val selectedEpInPlaylist = selectedEp.id in playlistEpisodeIds

                EpisodeMoreMenuBottomSheet(
                    episode = selectedEp,
                    scholar = state.scholar,
                    onDismiss = { selectedEpisodeForMenu = null },
                    downloadState = selectedEpDownloadState,
                    isFavorite = selectedEpIsFav,
                    onToggleFavorite = { viewModel.toggleFavorite(selectedEp.id) },
                    isInPlaylist = selectedEpInPlaylist,
                    onTogglePlaylist = { viewModel.togglePlaylist(selectedEp.id) },
                    onDownloadClick = { viewModel.downloadEpisode(selectedEp) },
                    onCancelDownloadClick = { viewModel.cancelDownload(selectedEp.id) },
                    onDeleteDownloadClick = { viewModel.deleteDownloadedEpisode(selectedEp) }
                )
            }

            // Dialog xác nhận tải xuống playlist
            if (state.showDownloadPlaylistDialog) {
                val batchCount = state.pendingBatchCount
                val scholarName = state.scholar?.name ?: "học giả"
                AlertDialog(
                    onDismissRequest = viewModel::dismissPlaylistDialogs,
                    title = {
                        Text(
                            text = "Xác nhận tải playlist",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = if (batchCount > 0) {
                                "Bạn có muốn tải xuống $batchCount tập mới nhất chưa có offline của $scholarName để nghe ngoại tuyến không?\n(Dung lượng ước tính: ~${batchCount * 15} MB)"
                            } else {
                                "Tất cả các tập hiện tại của $scholarName đã được tải xuống offline."
                            },
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    },
                    confirmButton = {
                        if (batchCount > 0) {
                            TextButton(onClick = viewModel::confirmDownloadPlaylist) {
                                Text("Tải xuống", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissPlaylistDialogs) {
                            Text(if (batchCount > 0) "Hủy" else "Đóng", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            // Dialog xác nhận dừng/hủy tải playlist
            if (state.showCancelPlaylistDialog) {
                val scholarName = state.scholar?.name ?: "học giả"
                AlertDialog(
                    onDismissRequest = viewModel::dismissPlaylistDialogs,
                    title = {
                        Text(
                            text = "Dừng tải playlist?",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Tiến trình tải playlist của $scholarName đang diễn ra. Bạn có muốn dừng và hủy danh sách chờ tải không?",
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::confirmCancelPlaylist) {
                            Text("Dừng tải", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissPlaylistDialogs) {
                            Text("Tiếp tục tải", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}
