package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.core.navigation.Screen
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel màn chi tiết học giả: header (bio, tổng số tập) + danh sách tập từ cache Room,
 * kèm refresh nền RSS. Điều khiển phát qua [AudioPlayerManager] dùng chung — phát tiếp
 * từ lastPositionMs, tua ±10s, đổi tốc độ.
 */
@HiltViewModel
class ScholarDetailViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val audioPlayerManager: AudioPlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val scholarId: String = savedStateHandle.get<String>(Screen.ScholarDetail.ARG_SCHOLAR_ID).orEmpty()

    data class UiState(
        val scholar: Scholar? = null,
        val episodes: List<PodcastEpisode> = emptyList(),
        val isLoading: Boolean = true,
        /** Đang fetch RSS nền (hiển thị indicator mảnh trên đầu danh sách). */
        val isRefreshing: Boolean = false,
        /** Fetch RSS thất bại (offline/feed lỗi) — banner nhỏ + nút thử lại. */
        val offlineError: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    // Pass-through trạng thái trình phát (giống SurahDetailViewModel).
    val isPlaying = audioPlayerManager.isPlaying
    val isBuffering = audioPlayerManager.isBuffering
    val currentEpisodeId = audioPlayerManager.currentMediaId
    val positionMs = audioPlayerManager.positionMs
    val durationMs = audioPlayerManager.durationMs
    val playbackSpeed = audioPlayerManager.playbackSpeed

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(scholar = podcastRepository.getScholarById(scholarId)) }
            // Offline-first: phát dữ liệu cache NGAY, sau đó refresh nền RSS.
            launch {
                podcastRepository.getEpisodesByScholar(scholarId).collect { episodes ->
                    _uiState.update { it.copy(episodes = episodes, isLoading = false) }
                }
            }
            refreshEpisodes()
        }
    }

    /** Fetch RSS nền; không xoá cache cũ khi lỗi — chỉ bật cờ để hiện banner nhỏ. */
    fun refreshEpisodes() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, offlineError = false) }
        viewModelScope.launch {
            val result = podcastRepository.refreshEpisodes(scholarId)
            _uiState.update { state ->
                state.copy(isRefreshing = false, offlineError = result.isFailure)
            }
        }
    }

    /** Phát/tạm dừng một tập; nếu tập khác đang phát thì chuyển sang tập được chọn. */
    fun onPlayPauseClicked(episode: PodcastEpisode, scholarName: String?) {
        if (currentEpisodeId.value == episode.id) {
            if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
            return
        }
        // Phát tiếp tục: chỉ resume khi còn vị trí hợp lệ (>0s và chưa gần hết tập).
        val nearEnd = episode.duration > 0 && episode.lastPositionMs >= episode.duration - 15_000L
        val startPosition = episode.lastPositionMs.takeIf { it > 0 && !nearEnd } ?: 0L
        audioPlayerManager.playPodcast(
            url = episode.audioUrl,
            mediaId = episode.id,
            startPositionMs = startPosition,
            title = episode.title,
            artist = scholarName,
            artworkPath = _uiState.value.scholar?.avatarPath
        )
    }

    fun seekForward() = audioPlayerManager.seekForward()

    fun seekBackward() = audioPlayerManager.seekBackward()

    fun cyclePlaybackSpeed(): Float = audioPlayerManager.cyclePlaybackSpeed()
}
