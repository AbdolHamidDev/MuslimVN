package com.example.muslimvn.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/** Snapshot trạng thái phát hiện tại dùng cho mini-player; null khi không có tập nào. */
data class ActivePlayback(
    val title: String,
    val subtitle: String?,
    val artworkPath: String?,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val speed: Float
)

/**
 * Gom toàn bộ StateFlow của [PodcastPlayerViewModel] thành một [ActivePlayback]
 * (hoặc null nếu không có tập đang phát) để các màn hình tái sử dụng cho mini-player.
 */
@Composable
fun PodcastPlayerBarState(
    playerViewModel: PodcastPlayerViewModel,
    content: @Composable (ActivePlayback?) -> Unit
) {
    val currentEpisodeId by playerViewModel.currentEpisodeId.collectAsState()
    val title by playerViewModel.title.collectAsState()
    val artist by playerViewModel.artist.collectAsState()
    val artworkPath by playerViewModel.artworkPath.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isBuffering by playerViewModel.isBuffering.collectAsState()
    val positionMs by playerViewModel.positionMs.collectAsState()
    val durationMs by playerViewModel.durationMs.collectAsState()
    val speed by playerViewModel.playbackSpeed.collectAsState()

    val active = if (currentEpisodeId == null || title == null) {
        null
    } else {
        ActivePlayback(
            title = title.orEmpty(),
            subtitle = artist,
            artworkPath = artworkPath,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            positionMs = positionMs,
            durationMs = durationMs,
            speed = speed
        )
    }
    content(active)
}
