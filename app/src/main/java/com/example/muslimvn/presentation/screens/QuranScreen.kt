package com.example.muslimvn.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.domain.models.availableReciters
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.MiniPlayerBar
import com.example.muslimvn.presentation.components.PodcastPlayerBarState
import com.example.muslimvn.presentation.components.ReciterSelectionDialog
import com.example.muslimvn.presentation.components.ShimmerPlaceholder
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import com.example.muslimvn.presentation.viewmodels.QuranViewModel
import com.example.muslimvn.ui.theme.extendedTypography
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    onSurahClick: (Int, Int) -> Unit,
    onSettingsClick: () -> Unit = {},
    onOpenFullPlayer: (String?) -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel(),
    playerViewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val hasSelectedReciter by viewModel.hasSelectedReciter.collectAsStateWithLifecycle()
    val quranSettings by viewModel.quranSettings.collectAsStateWithLifecycle()

    // Dialog chọn Qari lần đầu
    if (!hasSelectedReciter) {
        ReciterSelectionDialog(
            onReciterSelected = viewModel::onReciterSelected,
            onDismiss = { /* Tuỳ chọn: Có cho phép bỏ qua không? */ }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PodcastPlayerBarState(playerViewModel) { active ->
                val isQuran = active?.id?.contains(":") == true
                androidx.compose.animation.AnimatedVisibility(visible = isQuran) {
                    if (active != null) {
                        
                        MiniPlayerBar(
                            title = active.title,
                            subtitle = active.subtitle,
                            artworkPath = active.artworkPath,
                            isPlaying = active.isPlaying,
                            isBuffering = active.isBuffering,
                            positionMs = active.positionMs,
                            durationMs = active.durationMs,
                            speedLabel = formatSpeedLabel(active.speed),
                            onPlayPauseClick = if (isQuran) viewModel::togglePlayPause else playerViewModel::togglePlayPause,
                            onSeekTo = { pos -> if (!isQuran) playerViewModel.seekTo(pos) },
                            onCycleSpeed = { if (!isQuran) playerViewModel.cyclePlaybackSpeed() },
                            onOpenFullPlayer = { onOpenFullPlayer(active.id) },
                            currentMediaId = active.id,
                            playlist = emptyList(),
                            onPlayEpisode = { /* Implement if needed */ }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Thanh tiêu đề + Tìm kiếm tích hợp kiểu Google (không khoảng cách thừa)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text(stringResource(R.string.search_surah_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) { }
            }

            if (surahs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (searchQuery.isBlank()) {
                        QuranLoadingSkeleton()
                    } else {
                        EmptyState(
                            message = stringResource(R.string.search_no_results),
                            hint = stringResource(R.string.search_no_results_hint)
                        )
                    }
                }
            } else {
                val playingMediaId by viewModel.currentMediaId.collectAsStateWithLifecycle()
                val isPlayingAudio by viewModel.isPlaying.collectAsStateWithLifecycle()
                val currentReciter = availableReciters.find { it.identifier == quranSettings.reciterIdentifier } 
                    ?: availableReciters[0]

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(surahs, key = { it.number }) { surah ->
                        val isThisSurahPlaying = playingMediaId?.startsWith("${surah.number}:") == true
                        
                        SurahItem(
                            surah = surah,
                            isPlaying = isThisSurahPlaying && isPlayingAudio,
                            reciterImageUrl = if (isThisSurahPlaying) currentReciter?.imageUrl else null,
                            onClick = { onSurahClick(surah.number, 1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranLoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(72.dp))
        }
    }
}

@Composable
fun SurahItem(
    surah: Surah,
    isPlaying: Boolean = false,
    reciterImageUrl: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface
            )
            .bouncyClick(pressedScale = 0.99f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    if (reciterImageUrl != null) {
                        val imageModel = remember(reciterImageUrl) { reciterImageUrl.toAndroidAssetUri() }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f))
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameVietnamese,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.ayahs_revelation, surah.totalAyahs, surah.revelationType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = surah.nameArabic,
                style = MaterialTheme.extendedTypography.arabicInline,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}
