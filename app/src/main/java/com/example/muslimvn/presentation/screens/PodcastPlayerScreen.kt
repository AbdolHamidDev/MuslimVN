package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.formatDurationMs
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/**
 * Trình phát podcast TOÀN MÀN HÌNH: ảnh bìa lớn, thanh tua chi tiết kèm thời gian,
 * cụm điều khiển -10s / play-pause / +10s và nút xoay vòng tốc độ phát.
 * Không có tập đang phát -> hiện trạng thái rỗng rồi tự đóng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastPlayerScreen(
    onCloseClick: () -> Unit = {},
    viewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val currentEpisodeId by viewModel.currentEpisodeId.collectAsState()
    val title by viewModel.title.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val artworkPath by viewModel.artworkPath.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    // Vào màn mà không có phiên phát nào -> tự quay lại sau một nhịp ngắn.
    LaunchedEffect(currentEpisodeId) {
        if (currentEpisodeId == null) onCloseClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_podcast)) },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.player_close)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (currentEpisodeId == null || title == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(message = stringResource(R.string.podcast_empty_player))
            }
            return@Scaffold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            AsyncImage(
                model = artworkPath?.toAndroidAssetUri(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            artist?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Thanh tua + mốc thời gian đã nghe / còn lại.
            var dragValue by remember { mutableStateOf<Float?>(null) }
            val shownPosition = dragValue?.let { (it * durationMs).toLong() } ?: positionMs
            Slider(
                value = if (durationMs > 0) (shownPosition.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { viewModel.seekTo((it * durationMs).toLong()) }
                    dragValue = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDurationMs(shownPosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "-${formatDurationMs((durationMs - shownPosition).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                IconButton(onClick = viewModel::seekBackward, modifier = Modifier.size(52.dp)) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = stringResource(R.string.skip_back_10s),
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                FilledIconButton(
                    onClick = viewModel::togglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(76.dp)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = viewModel::seekForward, modifier = Modifier.size(52.dp)) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = stringResource(R.string.skip_forward_10s),
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            AssistChip(
                onClick = viewModel::cyclePlaybackSpeed,
                label = {
                    Text(
                        text = formatSpeedLabel(speed),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                shape = RoundedCornerShape(50)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
