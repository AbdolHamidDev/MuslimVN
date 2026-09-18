package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import com.example.muslimvn.data.local.dao.ScholarDao
import com.example.muslimvn.data.local.entities.PodcastEpisodeEntity
import com.example.muslimvn.data.local.entities.ScholarEntity
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.PodcastDownloadManager
import com.example.muslimvn.domain.models.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class DownloadedPodcastItem(
    val episode: PodcastEpisode,
    val scholarName: String,
    val fileSizeBytes: Long
)

/** Format dung lượng bộ nhớ (B, KB, MB, GB). */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        String.format(Locale.US, "%.2f GB", mb / 1024)
    } else if (mb >= 0.1) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", bytes.toDouble() / 1024)
    }
}

@HiltViewModel
@androidx.media3.common.util.UnstableApi
class DownloadedPodcastsViewModel @Inject constructor(
    private val episodeDao: PodcastEpisodeDao,
    private val scholarDao: ScholarDao,
    private val podcastDownloadManager: PodcastDownloadManager,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    val downloadedPodcasts: StateFlow<List<DownloadedPodcastItem>> = combine(
        episodeDao.getDownloadedEpisodes(),
        scholarDao.getAllScholars()
    ) { entities: List<PodcastEpisodeEntity>, scholars: List<ScholarEntity> ->
        val scholarMap = scholars.associateBy { it.id }
        entities.mapNotNull { entity ->
            val file = podcastDownloadManager.getDownloadedFile(entity.id)
            if (file.exists() && file.length() > 0) {
                DownloadedPodcastItem(
                    episode = PodcastEpisode(
                        id = entity.id,
                        scholarId = entity.scholarId,
                        title = entity.title,
                        audioUrl = entity.audioUrl,
                        artworkUrl = entity.artworkUrl,
                        duration = entity.duration,
                        pubDate = entity.pubDate,
                        description = entity.description,
                        isDownloaded = true,
                        lastPositionMs = entity.lastPositionMs,
                        localFilePath = file.absolutePath,
                        downloadStatus = "DOWNLOADED"
                    ),
                    scholarName = scholarMap[entity.scholarId]?.name ?: "Học giả Islam",
                    fileSizeBytes = file.length()
                )
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSizeBytes: StateFlow<Long> = downloadedPodcasts.map { list ->
        list.sumOf { it.fileSizeBytes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun playEpisode(item: DownloadedPodcastItem) {
        viewModelScope.launch {
            audioPlayerManager.playPodcast(
                url = item.episode.localFilePath ?: item.episode.audioUrl,
                mediaId = item.episode.id,
                startPositionMs = item.episode.lastPositionMs,
                title = item.episode.title,
                artist = item.scholarName,
                artworkPath = item.episode.artworkUrl
            )
        }
    }

    fun deleteEpisode(item: DownloadedPodcastItem) {
        podcastDownloadManager.deleteDownloadedFile(item.episode)
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            val items = downloadedPodcasts.value
            for (item in items) {
                podcastDownloadManager.deleteDownloadedFile(item.episode)
            }
            // Dọn dẹp tất cả file .mp3 hoặc .tmp sót lại trong thư mục storage
            runCatching {
                val dir = podcastDownloadManager.getDownloadDir()
                dir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                }
            }
        }
    }
}
