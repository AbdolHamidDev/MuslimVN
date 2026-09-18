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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muslimvn.data.util.PodcastDownloadState
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.screens.scholar.components.EpisodeMoreMenuBottomSheet
import com.example.muslimvn.presentation.screens.scholar.components.EpisodeRow
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.screens.scholar.components.LibraryDetailHeader
import com.example.muslimvn.presentation.screens.scholar.components.rememberDynamicBackgroundColor
import com.example.muslimvn.presentation.viewmodels.PodcastLibraryViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Màn hình danh sách các tập Podcast trong Danh sách phát / Hàng chờ (Playlist Screen):
 * - Edge-to-Edge tràn viền với nút Back tròn nổi bán trong suốt.
 * - Danh sách các tập podcast đã lưu vào danh sách chờ cá nhân.
 */
@Composable
fun PodcastPlaylistScreen(
    onBackClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: PodcastLibraryViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val playlistEpisodes by viewModel.playlistEpisodes.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val favoriteEpisodeIds by playerViewModel.favoriteEpisodeIds.collectAsStateWithLifecycle()
    val playlistEpisodeIds by viewModel.playlistEpisodeIds.collectAsStateWithLifecycle()

    var selectedEpisodeForMenu by remember { mutableStateOf<PodcastEpisode?>(null) }

    val imagePath = "images/library/playlist.webp"
    val backgroundColor = rememberDynamicBackgroundColor(imagePath)

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
                .background(backgroundColor)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = padding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Immersive Hero Header với ảnh playlist.webp + Pill Bar 3 Nút UI + Dynamic Background Color
                item(key = "header") {
                    val currentPlayingId by playerViewModel.currentEpisodeId.collectAsStateWithLifecycle()
                    val playerIsPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
                    val isPlaylistPlaying = playerIsPlaying && playlistEpisodes.any { it.episode.id == currentPlayingId }

                    LibraryDetailHeader(
                        title = "Danh sách phát",
                        subtitle = "${playlistEpisodes.size} tập trong hàng chờ cá nhân",
                        imagePath = imagePath,
                        backgroundColor = backgroundColor,
                        isPlaying = isPlaylistPlaying,
                        onPlayAllClick = viewModel::onPlayAllClicked,
                        onShuffleClick = viewModel::onShuffleClicked
                    )
                }

                if (playlistEpisodes.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            message = "Chưa có tập nào trong danh sách phát",
                            hint = "Mở menu tùy chọn (...) của tập bất kỳ và chọn 'Thêm vào danh sách phát' để lưu tại đây"
                        )
                    }
                } else {
                    items(playlistEpisodes, key = { it.episode.id }) { item ->
                        val currentEpisodeId by playerViewModel.currentEpisodeId.collectAsStateWithLifecycle()
                        val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
                        val epState = downloadStates[item.episode.id]
                            ?: if (item.episode.isDownloaded) PodcastDownloadState.Downloaded else PodcastDownloadState.Idle

                        EpisodeRow(
                            episode = item.episode,
                            isCurrent = currentEpisodeId == item.episode.id,
                            isPlaying = isPlaying,
                            downloadState = epState,
                            onPlay = { viewModel.playEpisode(item.episode, item.scholarName) },
                            onMoreClick = { selectedEpisodeForMenu = item.episode }
                        )
                    }
                }
            }

            // Nút Back tròn bán trong suốt nổi góc trên bên trái
            FloatingBackButton(
                onBackClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
            )

            // BottomSheet khi chọn menu 3 chấm
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
