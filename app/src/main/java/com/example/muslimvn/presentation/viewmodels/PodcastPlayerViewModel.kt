package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho trình phát podcast:
 * - Đồng bộ với AudioPlayerManager cho trạng thái phát hiện tại.
 * - Quản lý danh sách tập phát (playlist) của học giả hiện tại để hỗ trợ chuyển tập.
 */
@HiltViewModel
class PodcastPlayerViewModel @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager,
    private val repository: PodcastRepository
) : ViewModel() {

    val isPlaying = audioPlayerManager.isPlaying
    val isBuffering = audioPlayerManager.isBuffering
    val currentEpisodeId = audioPlayerManager.currentMediaId
    val positionMs = audioPlayerManager.positionMs
    val durationMs = audioPlayerManager.durationMs
    val playbackSpeed = audioPlayerManager.playbackSpeed
    val title = audioPlayerManager.nowPlayingTitle
    val artist = audioPlayerManager.nowPlayingArtist
    val artworkPath = audioPlayerManager.nowPlayingArtworkPath

    private val _playlist = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val playlist: StateFlow<List<PodcastEpisode>> = _playlist.asStateFlow()

    init {
        // Theo dõi tập phát hiện tại và nạp danh sách tập (playlist) tương ứng
        viewModelScope.launch {
            currentEpisodeId.collect { id ->
                if (id != null) {
                    val episode = repository.getEpisodeById(id)
                    if (episode != null) {
                        // Lắng nghe Flow thay vì chỉ lấy first() để đảm bảo playlist luôn cập nhật
                        repository.getEpisodesByScholar(episode.scholarId).collect { list ->
                            _playlist.value = list
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
    }

    fun seekForward() = audioPlayerManager.seekForward()
    fun seekBackward() = audioPlayerManager.seekBackward()
    fun seekTo(positionMs: Long) = audioPlayerManager.seekTo(positionMs)
    fun cyclePlaybackSpeed(): Float = audioPlayerManager.cyclePlaybackSpeed()

    fun playEpisode(episode: PodcastEpisode) {
        viewModelScope.launch {
            val scholar = repository.getScholarById(episode.scholarId)
            audioPlayerManager.playPodcast(
                url = episode.audioUrl,
                mediaId = episode.id,
                startPositionMs = episode.lastPositionMs,
                title = episode.title,
                artist = scholar?.name,
                artworkPath = episode.artworkUrl ?: scholar?.avatarPath
            )
        }
    }

    fun skipToNext() {
        val currentId = currentEpisodeId.value ?: return
        val list = _playlist.value
        val currentIndex = list.indexOfFirst { it.id == currentId }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playEpisode(list[currentIndex + 1])
        }
    }

    fun skipToPrevious() {
        val currentId = currentEpisodeId.value ?: return
        val list = _playlist.value
        val currentIndex = list.indexOfFirst { it.id == currentId }
        if (currentIndex > 0) {
            playEpisode(list[currentIndex - 1])
        }
    }
}
