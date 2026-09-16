package com.example.muslimvn.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel

/** Snapshot trạng thái phát hiện tại dùng cho mini-player; null khi không có tập nào. */
data class ActivePlayback(
    val id: String,
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
    val currentEpisodeId by playerViewModel.currentEpisodeId.collectAsStateWithLifecycle()
    val title by playerViewModel.title.collectAsStateWithLifecycle()
    val artist by playerViewModel.artist.collectAsStateWithLifecycle()
    val artworkPath by playerViewModel.artworkPath.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by playerViewModel.isBuffering.collectAsStateWithLifecycle()
    val positionMs by playerViewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by playerViewModel.durationMs.collectAsStateWithLifecycle()
    val speed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()

    val active = if (currentEpisodeId == null || title == null) {
        null
    } else {
        ActivePlayback(
            id = currentEpisodeId.orEmpty(),
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
