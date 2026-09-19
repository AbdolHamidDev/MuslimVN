package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.local.dao.ScholarDao
import com.example.muslimvn.data.local.entities.ScholarEntity
import com.example.muslimvn.data.util.AudioPlayItem
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.PodcastDownloadManager
import com.example.muslimvn.data.util.PodcastDownloadState
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteEpisodeItem(
    val episode: PodcastEpisode,
    val scholarName: String
)

@HiltViewModel
@androidx.media3.common.util.UnstableApi
class PodcastLibraryViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val scholarDao: ScholarDao,
    private val podcastDownloadManager: PodcastDownloadManager,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    val favoriteEpisodes: StateFlow<List<FavoriteEpisodeItem>> = combine(
        podcastRepository.getFavoriteEpisodes(),
        scholarDao.getAllScholars()
    ) { episodes: List<PodcastEpisode>, scholars: List<ScholarEntity> ->
        val scholarMap = scholars.associateBy { it.id }
        episodes.map { ep ->
            FavoriteEpisodeItem(
                episode = ep,
                scholarName = scholarMap[ep.scholarId]?.name ?: "Học giả Islam"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistEpisodes: StateFlow<List<FavoriteEpisodeItem>> = combine(
        podcastRepository.getPlaylistEpisodes(),
        scholarDao.getAllScholars()
    ) { episodes: List<PodcastEpisode>, scholars: List<ScholarEntity> ->
        val scholarMap = scholars.associateBy { it.id }
        episodes.map { ep ->
            FavoriteEpisodeItem(
                episode = ep,
                scholarName = scholarMap[ep.scholarId]?.name ?: "Học giả Islam"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistEpisodeIds: StateFlow<List<String>> = podcastRepository.getPlaylistEpisodeIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedEpisodes: StateFlow<List<FavoriteEpisodeItem>> = combine(
        podcastRepository.getRecentlyPlayedEpisodes(20),
        scholarDao.getAllScholars()
    ) { episodes: List<PodcastEpisode>, scholars: List<ScholarEntity> ->
        val scholarMap = scholars.associateBy { it.id }
        episodes.map { ep ->
            FavoriteEpisodeItem(
                episode = ep,
                scholarName = scholarMap[ep.scholarId]?.name ?: "Học giả Islam"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scholars: StateFlow<List<Scholar>> = podcastRepository.getScholars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadStates: StateFlow<Map<String, PodcastDownloadState>> = podcastDownloadManager.downloadStates

    fun toggleFavorite(episodeId: String) {
        viewModelScope.launch {
            podcastRepository.toggleFavorite(episodeId)
        }
    }

    fun togglePlaylist(episodeId: String) {
        viewModelScope.launch {
            podcastRepository.togglePlaylist(episodeId)
        }
    }

    fun playEpisode(episode: PodcastEpisode, scholarName: String?) {
        viewModelScope.launch {
            audioPlayerManager.playPodcast(
                url = episode.localFilePath ?: episode.audioUrl,
                mediaId = episode.id,
                startPositionMs = episode.lastPositionMs.takeIf { it > 0 } ?: 0L,
                title = episode.title,
                artist = scholarName ?: "Học giả Islam",
                artworkPath = episode.artworkUrl
            )
        }
    }

    fun downloadEpisode(episode: PodcastEpisode) {
        podcastDownloadManager.enqueueEpisode(episode, isPriority = true)
    }

    fun cancelDownload(episodeId: String) {
        podcastDownloadManager.cancelDownload(episodeId)
    }

    fun deleteDownloadedEpisode(episode: PodcastEpisode) {
        podcastDownloadManager.deleteDownloadedFile(episode)
    }

    fun clearHistoryItem(episodeId: String) {
        viewModelScope.launch {
            podcastRepository.clearHistoryItem(episodeId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            podcastRepository.clearAllHistory()
        }
    }

    /** Phát danh sách các tập yêu thích tuần tự. */
    fun onPlayAllClicked() {
        val episodes = favoriteEpisodes.value.map { it.episode }
        if (episodes.isEmpty()) return

        val currentId = audioPlayerManager.currentMediaId.value
        val isActive = episodes.any { it.id == currentId }

        if (isActive && audioPlayerManager.isPlaying.value) {
            audioPlayerManager.pause()
        } else {
            val items = episodes.map { ep ->
                AudioPlayItem(
                    url = ep.localFilePath ?: ep.audioUrl,
                    mediaId = ep.id,
                    title = ep.title,
                    artist = "Thư viện Podcast",
                    artworkPath = ep.artworkUrl
                )
            }
            audioPlayerManager.playList(
                items = items,
                startIndex = 0,
                startPositionMs = episodes[0].lastPositionMs.takeIf { it > 0 } ?: 0L,
                isPodcast = true
            )
        }
    }

    /** Trộn phát ngẫu nhiên danh sách các tập yêu thích. */
    fun onShuffleClicked() {
        val episodes = favoriteEpisodes.value.map { it.episode }.shuffled()
        if (episodes.isEmpty()) return

        val items = episodes.map { ep ->
            AudioPlayItem(
                url = ep.localFilePath ?: ep.audioUrl,
                mediaId = ep.id,
                title = ep.title,
                artist = "Thư viện Podcast",
                artworkPath = ep.artworkUrl
            )
        }
        audioPlayerManager.playList(
            items = items,
            startIndex = 0,
            startPositionMs = 0L,
            isPodcast = true
        )
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val scholarList = scholars.value
                for (scholar in scholarList) {
                    podcastRepository.refreshEpisodes(scholar.id)
                }
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
