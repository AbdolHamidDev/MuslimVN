@file:OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)

package com.example.muslimvn.presentation.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muslimvn.R
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.screens.podcast.components.BrandSectionTitle
import com.example.muslimvn.presentation.screens.podcast.components.CategoryChipsRow
import com.example.muslimvn.presentation.screens.podcast.components.FeaturedScholarsSection
import com.example.muslimvn.presentation.screens.podcast.components.MuslimCentralSection
import com.example.muslimvn.presentation.screens.podcast.components.SectionTitle
import com.example.muslimvn.presentation.screens.scholar.components.FloatingBackButton
import com.example.muslimvn.presentation.screens.scholar.components.FloatingLibraryButton
import com.example.muslimvn.presentation.viewmodels.PodcastHomeViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Trang chủ Podcast học giả Islam phong cách Edge-to-Edge tràn viền cao cấp:
 * - Hàng FilterChip phân loại bo tròn dạng viên thuốc.
 * - Carousel Hero Cards "Học giả nổi bật" lớn đầy ấn tượng (Mufti Menk số 1, Hamza Yusuf số 2).
 * - Danh sách học giả thương hiệu Muslim Central dạng Carousel cuộn ngang Hero Cards có hiệu ứng ám mờ 2 bên.
 * - Nút mũi tên bên phải tiêu đề Muslim Central mở màn hình Lưới 2 cột tất cả học giả.
 * - Nút Thư viện Podcast (Library Button) ở góc trên bên phải.
 * - Mini-player dính đáy khi có tập đang phát.
 */
@Composable
fun PodcastHomeScreen(
    onBackClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onScholarClick: (String) -> Unit = {},
    onSeeAllMuslimCentralClick: () -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: PodcastHomeViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteEpisodeIds by playerViewModel.favoriteEpisodeIds.collectAsStateWithLifecycle()

    // Sắp xếp thứ tự Danh sách Nổi bật: 1. Mufti Menk, 2. Hamza Yusuf
    val orderedFeaturedScholars = remember(state.featuredScholars) {
        val priorityOrder = listOf("mufti-menk", "hamza-yusuf")
        state.featuredScholars.sortedBy { scholar ->
            val index = priorityOrder.indexOf(scholar.id)
            if (index != -1) index else Int.MAX_VALUE
        }
    }

    // Deduplication: Lọc bỏ các học giả đã xuất hiện trong danh sách "Nổi bật" & Mufti Menk khỏi Muslim Central
    val featuredIds = remember(orderedFeaturedScholars) {
        orderedFeaturedScholars.map { it.id }.toSet()
    }
    val muslimCentralScholars = remember(state.scholars, featuredIds) {
        state.scholars.filter { scholar ->
            scholar.id !in featuredIds && scholar.id != "mufti-menk"
        }
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
            when {
                state.isLoading -> LoadingIndicator(
                    label = stringResource(R.string.loading_please_wait),
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.fillMaxSize()
                )
                state.loadError && state.scholars.isEmpty() -> ErrorState(
                    message = stringResource(R.string.podcast_load_error),
                    onRetry = viewModel::retryLoading,
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = padding.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tiêu đề trang Magazine Header nằm dưới nút Back floating
                    item(key = "top_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.podcast_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Hàng chip phân loại
                    item(key = "chips") {
                        CategoryChipsRow(
                            categories = state.categories,
                            selectedCategoryId = state.selectedCategoryId,
                            onCategorySelected = viewModel::selectCategory
                        )
                    }

                    // Phần học giả nổi bật (khi không lọc hoặc chọn chip "Tất cả")
                    if (orderedFeaturedScholars.isNotEmpty() && state.selectedCategoryId == null) {
                        item(key = "featured_title") {
                            SectionTitle(text = stringResource(R.string.podcast_featured_scholars))
                        }
                        item(key = "featured_row") {
                            FeaturedScholarsSection(
                                scholars = orderedFeaturedScholars,
                                onScholarClick = onScholarClick
                            )
                        }
                    }

                    // Tiêu đề danh sách chính Muslim Central kèm Nút Mũi Tên Mở Trang Mới
                    item(key = "all_title") {
                        if (state.selectedCategoryId == null) {
                            BrandSectionTitle(
                                title = "Muslim Central",
                                logoPath = "images/brand/muslim_central.webp",
                                onSeeAllClick = onSeeAllMuslimCentralClick
                            )
                        } else {
                            SectionTitle(
                                text = state.categories.find { it.id == state.selectedCategoryId }?.name
                                    ?: stringResource(R.string.podcast_all_scholars)
                            )
                        }
                    }

                    // Danh sách học giả Muslim Central dạng Hero Cards cuộn ngang cao cấp
                    item(key = "muslim_central_row") {
                        MuslimCentralSection(
                            scholars = if (state.selectedCategoryId == null) muslimCentralScholars else state.scholars,
                            onScholarClick = onScholarClick
                        )
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

            // Nút Thư viện Podcast (Library Button) nổi góc trên bên phải
            FloatingLibraryButton(
                onLibraryClick = onLibraryClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 12.dp)
            )
        }
    }
}
