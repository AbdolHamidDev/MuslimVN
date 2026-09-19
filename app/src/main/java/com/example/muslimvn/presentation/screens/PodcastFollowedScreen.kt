@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

package com.example.muslimvn.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.screens.podcast.components.MuslimCentralScholarCard
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.screens.scholar.components.LibraryDetailHeader
import com.example.muslimvn.presentation.screens.scholar.components.PodcastSearchBar
import com.example.muslimvn.presentation.screens.scholar.components.rememberDynamicBackgroundColor
import com.example.muslimvn.presentation.viewmodels.PodcastLibraryViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Màn hình danh sách Học giả / Kênh Podcast đã theo dõi (Followed Scholars Screen):
 * - Edge-to-Edge tràn viền với nút Back tròn nổi bán trong suốt.
 */
@Composable
fun PodcastFollowedScreen(
    onBackClick: () -> Unit = {},
    onScholarClick: (String) -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: PodcastLibraryViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val scholars by viewModel.scholars.collectAsStateWithLifecycle()
    val favoriteEpisodeIds by playerViewModel.favoriteEpisodeIds.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredScholars = remember(scholars, searchQuery) {
        if (searchQuery.isBlank()) {
            scholars
        } else {
            scholars.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.bio.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val imagePath = "images/library/Following.webp"
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = padding.calculateBottomPadding() + 24.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Immersive Hero Header với ảnh Following.webp + Pill Bar 3 Nút UI + Dynamic Background Color
                item(span = { GridItemSpan(2) }, key = "header") {
                    LibraryDetailHeader(
                        title = "Đã theo dõi",
                        subtitle = "${scholars.size} học giả & kênh podcast",
                        imagePath = imagePath,
                        backgroundColor = backgroundColor,
                        onSearchClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }
                    )
                }

                // Thanh tìm kiếm hiển thị khi bấm icon Search trên Pill Header
                item(span = { GridItemSpan(2) }, key = "search_bar") {
                    AnimatedVisibility(visible = isSearchActive) {
                        PodcastSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Tìm kiếm học giả đã theo dõi..."
                        )
                    }
                }

                if (scholars.isEmpty()) {
                    item(span = { GridItemSpan(2) }, key = "empty") {
                        EmptyState(
                            message = "Chưa theo dõi kênh nào",
                            hint = "Khám phá các học giả Islam trên trang chủ và nhấn Theo dõi để lưu vào đây"
                        )
                    }
                } else if (filteredScholars.isEmpty()) {
                    item(span = { GridItemSpan(2) }, key = "empty_search") {
                        EmptyState(
                            message = "Không tìm thấy kết quả cho \"$searchQuery\"",
                            hint = "Thử tìm kiếm với từ khóa khác"
                        )
                    }
                } else {
                    items(filteredScholars, key = { it.id }) { scholar ->
                        MuslimCentralScholarCard(
                            scholar = scholar,
                            onClick = { onScholarClick(scholar.id) },
                            modifier = Modifier.fillMaxWidth()
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
        }
    }
}
