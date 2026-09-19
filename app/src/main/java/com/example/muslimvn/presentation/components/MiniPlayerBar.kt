package com.example.muslimvn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PodcastEpisode

/**
 * Mini-player dính đáy: hiện khi có tập podcast đang phát.
 * Dạng nổi (floating) kiểu YouTube Music, hỗ trợ lướt để chuyển tập.
 * Hỗ trợ tùy chỉnh containerColor để đồng bộ màu nền dynamic với màn chi tiết học giả.
 */
@Composable
fun MiniPlayerBar(
    title: String,
    subtitle: String?,
    artworkPath: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    speedLabel: String,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    currentMediaId: String? = null,
    playlist: List<PodcastEpisode> = emptyList(),
    onPlayEpisode: (PodcastEpisode) -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    containerColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val barColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val titleColor = if (containerColor != null) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (containerColor != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (containerColor != null) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = barColor
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thanh tiến trình siêu mảnh ở trên cùng
            val fraction = positionMs.toFloat().div(durationMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .background(Color.White)
                )
            }

            // Pager cho nội dung Mini Player
            val initialPage = remember(playlist, currentMediaId) {
                val index = playlist.indexOfFirst { it.id == currentMediaId }
                if (index == -1) 0 else index
            }
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { if (playlist.isEmpty()) 1 else playlist.size }
            )

            val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

            LaunchedEffect(currentMediaId, playlist) {
                if (playlist.isNotEmpty()) {
                    val target = playlist.indexOfFirst { it.id == currentMediaId }
                    if (target != -1 && target != pagerState.currentPage) {
                        pagerState.scrollToPage(target)
                    }
                }
            }

            LaunchedEffect(isDragged) {
                if (isDragged) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        if (playlist.isNotEmpty() && page < playlist.size) {
                            val episode = playlist[page]
                            if (episode.id != currentMediaId) {
                                onPlayEpisode(episode)
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClick(onClick = onOpenFullPlayer, pressedScale = 0.98f)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = playlist.size > 1
                ) { page ->
                    val episode = playlist.getOrNull(page)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = (episode?.artworkUrl ?: artworkPath)?.toAndroidAssetUri(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = episode?.title ?: title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Bỏ khỏi yêu thích" else "Thêm vào yêu thích",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onPlayPauseClick) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
