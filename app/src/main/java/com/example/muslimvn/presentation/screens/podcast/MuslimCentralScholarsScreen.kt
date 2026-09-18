@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

package com.example.muslimvn.presentation.screens.podcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.screens.podcast.components.BrandSectionTitle
import com.example.muslimvn.presentation.screens.podcast.components.CategoryChipsRow
import com.example.muslimvn.presentation.screens.podcast.components.MuslimCentralScholarCard
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.viewmodels.PodcastHomeViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Màn hình danh sách tất cả học giả Muslim Central dạng Lưới 2 Cột (2-Column Grid):
 * - Thiết kế Edge-to-Edge tràn viền với nút Back hình tròn nổi bán trong suốt.
 * - Header thương hiệu Logo + Muslim Central và hàng chip lọc danh mục.
 * - Lưới 2 cột chứa các Card học giả tràn viền cao cấp.
 */
@Composable
fun MuslimCentralScholarsScreen(
    onBackClick: () -> Unit = {},
    onScholarClick: (String) -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: PodcastHomeViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Deduplication: Lọc bỏ các học giả Nổi bật & Mufti Menk
    val featuredIds = remember(state.featuredScholars) {
        state.featuredScholars.map { it.id }.toSet()
    }
    val muslimCentralScholars = remember(state.scholars, featuredIds) {
        state.scholars.filter { scholar ->
            scholar.id !in featuredIds && scholar.id != "mufti-menk"
        }
    }

    val displayScholars = if (state.selectedCategoryId == null) muslimCentralScholars else state.scholars

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
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                state.isLoading -> LoadingIndicator(
                    label = stringResource(R.string.loading_please_wait),
                    modifier = Modifier.fillMaxSize()
                )
                state.loadError && state.scholars.isEmpty() -> ErrorState(
                    message = stringResource(R.string.podcast_load_error),
                    onRetry = viewModel::retryLoading,
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = padding.calculateBottomPadding() + 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header thương hiệu nằm dưới nút Back floating (Chiếm toàn bộ 2 cột)
                    item(span = { GridItemSpan(2) }, key = "brand_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 56.dp, bottom = 4.dp)
                        ) {
                            BrandSectionTitle(
                                title = "Muslim Central",
                                logoPath = "images/brand/muslim_central.webp"
                            )
                        }
                    }

                    // Hàng chip phân loại (Chiếm toàn bộ 2 cột)
                    item(span = { GridItemSpan(2) }, key = "chips") {
                        CategoryChipsRow(
                            categories = state.categories,
                            selectedCategoryId = state.selectedCategoryId,
                            onCategorySelected = viewModel::selectCategory
                        )
                    }

                    // Danh sách học giả dạng Lưới 2 Cột
                    items(displayScholars, key = { it.id }) { scholar ->
                        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                            MuslimCentralScholarCard(
                                scholar = scholar,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onScholarClick(scholar.id) }
                            )
                        }
                    }
                }
            }

            // Nút Back tròn bán trong suốt đè góc trên bên trái
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
