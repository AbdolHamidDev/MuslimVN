package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.AudioPlayItem
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import com.example.muslimvn.data.util.PodcastDownloadManager
import com.example.muslimvn.data.util.PodcastDownloadState
import com.example.muslimvn.data.util.PlaylistDownloadProgress
import kotlinx.coroutines.launch

/**
 * ViewModel màn chi tiết học giả: header (bio, tổng số tập) + danh sách tập từ cache Room,
 * kèm refresh nền RSS. Điều khiển phát qua [AudioPlayerManager] dùng chung — phát tiếp
 * từ lastPositionMs, tua ±10s, đổi tốc độ. Quản lý tải xuống qua [PodcastDownloadManager].
 */
@androidx.media3.common.util.UnstableApi
@HiltViewModel(assistedFactory = ScholarDetailViewModel.Factory::class)
class ScholarDetailViewModel @AssistedInject constructor(
    private val podcastRepository: PodcastRepository,
    private val audioPlayerManager: AudioPlayerManager,
    private val podcastDownloadManager: PodcastDownloadManager,
    @Assisted val scholarId: String
) : ViewModel() {
    @AssistedFactory
    interface Factory { fun create(scholarId: String): ScholarDetailViewModel }

    data class UiState(
        val scholar: Scholar? = null,
        val isLoading: Boolean = true,
        val totalEpisodesCount: Int = 0,
        /** Đang fetch RSS nền (hiển thị indicator mảnh trên đầu danh sách). */
        val isRefreshing: Boolean = false,
        /** Fetch RSS thất bại (offline/feed lỗi) — banner nhỏ + nút thử lại. */
        val offlineError: Boolean = false,
        val showDownloadPlaylistDialog: Boolean = false,
        val showCancelPlaylistDialog: Boolean = false,
        val pendingBatchCount: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _scholarEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val scholarEpisodes = _scholarEpisodes.asStateFlow()

    // Theo dõi trạng thái tải xuống từng tập và tổng thể playlist của học giả
    val downloadStates: StateFlow<Map<String, PodcastDownloadState>> = podcastDownloadManager.downloadStates
    val playlistProgresses: StateFlow<Map<String, PlaylistDownloadProgress>> = podcastDownloadManager.playlistProgresses

    val playlistEpisodeIds: StateFlow<List<String>> = podcastRepository.getPlaylistEpisodeIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pass-through trạng thái trình phát (giống SurahDetailViewModel).
    val isPlaying = audioPlayerManager.isPlaying
    val isBuffering = audioPlayerManager.isBuffering
    val currentEpisodeId = audioPlayerManager.currentMediaId
    val positionMs = audioPlayerManager.positionMs
    val durationMs = audioPlayerManager.durationMs
    val playbackSpeed = audioPlayerManager.playbackSpeed

    val episodesPagingData: Flow<PagingData<PodcastEpisode>> =
        podcastRepository.getEpisodesByScholarPaging(scholarId)
            .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(scholar = podcastRepository.getScholarById(scholarId)) }
            // Theo dõi danh sách & số lượng tập từ Room (sắp xếp pubDate DESC - mới nhất lên đầu)
            launch {
                podcastRepository.getEpisodesByScholar(scholarId).collect { episodes ->
                    _scholarEpisodes.value = episodes
                    _uiState.update { it.copy(totalEpisodesCount = episodes.size, isLoading = false) }
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

    /**
     * Nút "Nghe" chính ở Header:
     * - Khi đang phát (isPlaying == true) -> Tạm dừng / Dừng.
     * - Khi đang dừng (isPlaying == false) -> Trở về chức năng cốt lõi: phát từ đầu danh sách mới nhất (index 0).
     */
    fun onPlayAllClicked() {
        val episodes = _scholarEpisodes.value
        if (episodes.isEmpty()) return

        val currentId = currentEpisodeId.value
        val isScholarEpisodeActive = episodes.any { it.id == currentId }

        if (isScholarEpisodeActive && isPlaying.value) {
            audioPlayerManager.pause()
        } else {
            val items = episodes.map { ep ->
                AudioPlayItem(
                    url = ep.audioUrl,
                    mediaId = ep.id,
                    title = ep.title,
                    artist = _uiState.value.scholar?.name,
                    artworkPath = ep.artworkUrl ?: _uiState.value.scholar?.avatarPath
                )
            }
            val firstEp = episodes[0]
            val nearEnd = firstEp.duration > 0 && firstEp.lastPositionMs >= firstEp.duration - 15_000L
            val startPosition = firstEp.lastPositionMs.takeIf { it > 0 && !nearEnd } ?: 0L

            audioPlayerManager.playList(
                items = items,
                startIndex = 0,
                startPositionMs = startPosition,
                isPodcast = true
            )
        }
    }

    /**
     * Nút "Nghe ngẫu nhiên" ở Header:
     * - Trộn ngẫu nhiên danh sách tập không bị lặp lại và phát.
     * - Nút ở giữa chuyển sang "Dừng" nhờ [isPlaying] và [currentEpisodeId].
     */
    fun onShuffleClicked() {
        val episodes = _scholarEpisodes.value
        if (episodes.isEmpty()) return

        val shuffledEpisodes = episodes.shuffled()
        val items = shuffledEpisodes.map { ep ->
            AudioPlayItem(
                url = ep.audioUrl,
                mediaId = ep.id,
                title = ep.title,
                artist = _uiState.value.scholar?.name,
                artworkPath = ep.artworkUrl ?: _uiState.value.scholar?.avatarPath
            )
        }
        val firstEp = shuffledEpisodes[0]
        val nearEnd = firstEp.duration > 0 && firstEp.lastPositionMs >= firstEp.duration - 15_000L
        val startPosition = firstEp.lastPositionMs.takeIf { it > 0 && !nearEnd } ?: 0L

        audioPlayerManager.playList(
            items = items,
            startIndex = 0,
            startPositionMs = startPosition,
            isPodcast = true
        )
    }

    /**
     * Phát/tạm dừng một tập cụ thể trong danh sách.
     * Khi bắt đầu tập mới, nạp toàn bộ danh sách từ tập được chọn trở đi vào player để tự động phát chuyển bài.
     */
    fun onPlayPauseClicked(episode: PodcastEpisode, scholarName: String?) {
        if (currentEpisodeId.value == episode.id) {
            if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
            return
        }

        val episodes = _scholarEpisodes.value
        val startIndex = episodes.indexOfFirst { it.id == episode.id }.coerceAtLeast(0)
        val targetEpisode = episodes.getOrNull(startIndex) ?: episode

        val nearEnd = targetEpisode.duration > 0 && targetEpisode.lastPositionMs >= targetEpisode.duration - 15_000L
        val startPosition = targetEpisode.lastPositionMs.takeIf { it > 0 && !nearEnd } ?: 0L

        val items = episodes.map { ep ->
            AudioPlayItem(
                url = ep.audioUrl,
                mediaId = ep.id,
                title = ep.title,
                artist = scholarName ?: _uiState.value.scholar?.name,
                artworkPath = ep.artworkUrl ?: _uiState.value.scholar?.avatarPath
            )
        }

        if (items.isNotEmpty()) {
            audioPlayerManager.playList(
                items = items,
                startIndex = startIndex,
                startPositionMs = startPosition,
                isPodcast = true
            )
        } else {
            audioPlayerManager.playPodcast(
                url = episode.audioUrl,
                mediaId = episode.id,
                startPositionMs = startPosition,
                title = episode.title,
                artist = scholarName,
                artworkPath = episode.artworkUrl ?: _uiState.value.scholar?.avatarPath
            )
        }
    }

    /** Yêu cầu tải xuống một tập podcast đơn lẻ. */
    fun downloadEpisode(episode: PodcastEpisode) {
        podcastDownloadManager.enqueueEpisode(episode, isPriority = true)
    }

    /** Hủy tiến trình tải xuống của một tập podcast. */
    fun cancelDownload(episodeId: String) {
        podcastDownloadManager.cancelDownload(episodeId)
    }

    /** Đảo trạng thái yêu thích của tập podcast. */
    fun toggleFavorite(episodeId: String) {
        viewModelScope.launch {
            podcastRepository.toggleFavorite(episodeId)
        }
    }

    /** Đảo trạng thái lưu tập vào danh sách phát cá nhân. */
    fun togglePlaylist(episodeId: String) {
        viewModelScope.launch {
            podcastRepository.togglePlaylist(episodeId)
        }
    }

    /** Xóa file audio offline của một tập podcast. */
    fun deleteDownloadedEpisode(episode: PodcastEpisode) {
        podcastDownloadManager.deleteDownloadedFile(episode)
    }

    /** Xử lý khi nhấn nút Tải playlist ở Header: kiểm tra trạng thái để hiện Dialog tương ứng. */
    fun downloadScholarPlaylist() {
        val currentProgress = playlistProgresses.value[scholarId]
        if (currentProgress?.isDownloading == true) {
            _uiState.update { it.copy(showCancelPlaylistDialog = true) }
        } else {
            val episodes = _scholarEpisodes.value
            val batch = podcastDownloadManager.getPendingBatch(episodes)
            _uiState.update {
                it.copy(
                    showDownloadPlaylistDialog = true,
                    pendingBatchCount = batch.size
                )
            }
        }
    }

    /** Xác nhận tải playlist từ Dialog. */
    fun confirmDownloadPlaylist() {
        _uiState.update { it.copy(showDownloadPlaylistDialog = false) }
        val episodes = _scholarEpisodes.value
        if (episodes.isNotEmpty()) {
            podcastDownloadManager.enqueuePlaylist(scholarId, episodes)
        }
    }

    /** Xác nhận hủy/dừng tiến trình tải playlist từ Dialog. */
    fun confirmCancelPlaylist() {
        _uiState.update { it.copy(showCancelPlaylistDialog = false) }
        podcastDownloadManager.cancelPlaylistDownload(scholarId)
    }

    /** Đóng các Dialog xác nhận. */
    fun dismissPlaylistDialogs() {
        _uiState.update {
            it.copy(
                showDownloadPlaylistDialog = false,
                showCancelPlaylistDialog = false
            )
        }
    }

    fun seekForward() = audioPlayerManager.seekForward()

    fun seekBackward() = audioPlayerManager.seekBackward()

    fun cyclePlaybackSpeed(): Float = audioPlayerManager.cyclePlaybackSpeed()
}
