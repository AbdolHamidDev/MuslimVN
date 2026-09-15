package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.repository.IslamHouseRepository
import com.example.muslimvn.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho trình phát podcast:
 * - Đồng bộ với AudioPlayerManager cho trạng thái phát hiện tại.
 * - Quản lý danh sách tập phát (playlist) của học giả hiện tại để hỗ trợ chuyển tập.
 * - Hỗ trợ cả Podcast cơ sở dữ liệu và tài liệu âm thanh Mách Zên (IslamHouse).
 */
@HiltViewModel
@androidx.media3.common.util.UnstableApi
class PodcastPlayerViewModel @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager,
    private val repository: PodcastRepository,
    private val islamHouseRepository: IslamHouseRepository
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
            currentEpisodeId.collectLatest { id ->
                if (id != null) {
                    if (id.startsWith("islamhouse_")) {
                        // Nạp danh sách bài giảng âm thanh từ IslamHouse cho Mách Zên
                        islamHouseRepository.getAuthorDocuments(193689, 1).collect { result ->
                            result.onSuccess { (docs, _) ->
                                val episodes = docs
                                    .filter { it.fileExtension?.equals("MP3", ignoreCase = true) == true && !it.downloadUrl.isNullOrBlank() }
                                    .map { doc ->
                                        PodcastEpisode(
                                            id = "islamhouse_${doc.id}",
                                            scholarId = "mach_zen",
                                            title = doc.title,
                                            audioUrl = doc.downloadUrl.orEmpty(),
                                            artworkUrl = "images/featured_scholars_vietnam/mach_zen.webp",
                                            duration = 0L,
                                            pubDate = doc.addDate?.times(1000) ?: 0L,
                                            description = doc.description.orEmpty(),
                                            isDownloaded = false,
                                            lastPositionMs = 0L
                                        )
                                    }
                                _playlist.value = episodes
                            }
                        }
                    } else {
                        val episode = repository.getEpisodeById(id)
                        if (episode != null) {
                            repository.getEpisodesByScholar(episode.scholarId).collect { list ->
                                _playlist.value = list
                            }
                        } else {
                            _playlist.value = emptyList()
                        }
                    }
                } else {
                    _playlist.value = emptyList()
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
            if (episode.id.startsWith("islamhouse_")) {
                val mp3List = _playlist.value
                val startIndex = mp3List.indexOfFirst { it.id == episode.id }.coerceAtLeast(0)
                val items = mp3List.map { ep ->
                    com.example.muslimvn.data.util.AudioPlayItem(
                        url = ep.audioUrl,
                        mediaId = ep.id,
                        title = ep.title,
                        artist = "Mách Zên",
                        artworkPath = ep.artworkUrl
                    )
                }
                if (items.isNotEmpty()) {
                    audioPlayerManager.playList(
                        items = items,
                        startIndex = startIndex,
                        isPodcast = true
                    )
                }
            } else {
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
    }

    fun skipToNext() {
        val currentId = currentEpisodeId.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == currentId }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playEpisode(list[currentIndex + 1])
        }
    }

    fun skipToPrevious() {
        val currentId = currentEpisodeId.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == currentId }
        if (currentIndex > 0) {
            playEpisode(list[currentIndex - 1])
        }
    }
}
