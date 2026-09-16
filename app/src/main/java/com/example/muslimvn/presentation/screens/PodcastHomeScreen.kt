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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PodcastCategory
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.viewmodels.PodcastHomeViewModel
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Trang chủ Podcast học giả Islam:
 * - Hàng FilterChip phân loại (Tafsir, Fiqh, Aqidah, Tazkiyah, Đương đại, Cảm hứng).
 * - Carousel ngang "Học giả nổi bật".
 * - Danh sách tất cả học giả: avatar asset, tên, chức danh và thẻ phân loại.
 * - Mini-player dính đáy khi có tập đang phát.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastHomeScreen(
    onBackClick: () -> Unit = {},
    onScholarClick: (String) -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    viewModel: PodcastHomeViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.podcast_title)) },
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
                val isPodcast = active != null && !active.id.contains(":") && !active.id.startsWith("islamhouse_")
                AnimatedVisibility(visible = isPodcast) {
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
            state.loadError && state.scholars.isEmpty() -> ErrorState(
                message = stringResource(R.string.podcast_load_error),
                onRetry = viewModel::retryLoading,
                modifier = Modifier.fillMaxSize()
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "chips") {
                    CategoryChipsRow(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = viewModel::selectCategory
                    )
                }
                if (state.featuredScholars.isNotEmpty()) {
                    item(key = "featured_title") {
                        SectionTitle(text = stringResource(R.string.podcast_featured_scholars))
                    }
                    item(key = "featured_row") {
                        FeaturedScholarsRow(
                            scholars = state.featuredScholars,
                            onScholarClick = onScholarClick
                        )
                    }
                }
                item(key = "all_title") {
                    SectionTitle(text = stringResource(R.string.podcast_all_scholars))
                }
                items(state.scholars, key = { it.id }) { scholar ->
                    ScholarListRow(scholar = scholar, onClick = { onScholarClick(scholar.id) })
                }
            }
        }
    }
}

/** Hàng chip lọc phân loại; chip đầu tiên là "Tất cả" (id = null). */
@Composable
private fun CategoryChipsRow(
    categories: List<PodcastCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all_chip") {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.podcast_category_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = {
                    // Bấm lại chip đang chọn -> bỏ chọn về "Tất cả".
                    onCategorySelected(if (selectedCategoryId == category.id) null else category.id)
                },
                label = { Text(category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun FeaturedScholarsRow(
    scholars: List<Scholar>,
    onScholarClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(scholars, key = { it.id }) { scholar ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(110.dp)
                    .bouncyClick { onScholarClick(scholar.id) }
            ) {
                Surface(
                    shape = CircleShape,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    AsyncImage(
                        model = scholar.avatarPath.toAndroidAssetUri(),
                        contentDescription = scholar.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = scholar.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = scholar.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** Hàng học giả trong danh sách chính: Thiết kế phẳng, hiện đại kiểu YT Music. */
@Composable
private fun ScholarListRow(scholar: Scholar, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AsyncImage(
                model = scholar.avatarPath.toAndroidAssetUri(),
                contentDescription = scholar.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scholar.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = scholar.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (scholar.tags.isNotEmpty()) {
                    Text(
                        text = scholar.tags.joinToString(" • ") { it.replaceFirstChar { c -> c.uppercase() } },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
