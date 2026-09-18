package com.example.muslimvn.presentation.screens.scholar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.muslimvn.R
import com.example.muslimvn.data.util.PodcastDownloadState
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.formatDurationMs
import com.example.muslimvn.presentation.components.formatPubDate

/**
 * Một dòng tập phát:
 * - Khi đang nghe (isCurrent), hiển thị Lottie voice wave animation tràn vừa tầm với kích thước chữ tiêu đề.
 * - Loại bỏ nền active highlight khi đang nghe.
 * - Tiêu đề tập podcast có hiệu ứng tự động trôi chữ (basicMarquee) nếu tiêu đề quá dài.
 * - Hiển thị trạng thái tải xuống offline.
 * - Nút 3 chấm bên phải để mở BottomSheet tùy chọn.
 */
@Composable
fun EpisodeRow(
    episode: PodcastEpisode,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    downloadState: PodcastDownloadState = PodcastDownloadState.Idle,
    onPlay: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .bouncyClick(onClick = onPlay)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Lottie wave voice animation dạng vòng tròn chuẩn 36.dp không bị cắt xén lề
            if (isCurrent) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.lottie_voice_line_wave_animation)
                )
                val lottieProgress by animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = isPlaying,
                    iterations = LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 6.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Tiêu đề tập podcast với hiệu ứng tự động trôi chữ mượt mà nếu văn bản quá dài
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    color = Color.White,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
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
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    val isDownloaded = episode.isDownloaded || downloadState is PodcastDownloadState.Downloaded
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = "Đã tải offline",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else when (downloadState) {
                        is PodcastDownloadState.Downloading -> {
                            Spacer(modifier = Modifier.width(6.dp))
                            CircularProgressIndicator(
                                strokeWidth = 1.5.dp,
                                color = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(downloadState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        is PodcastDownloadState.Queued -> {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\u2022 Đang chờ tải",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        is PodcastDownloadState.Failed -> {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Tải thất bại",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        else -> {}
                    }

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
                            color = Color(0xFFFFD700),
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Nút 3 chấm mở BottomSheet tùy chọn
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Tùy chọn",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
