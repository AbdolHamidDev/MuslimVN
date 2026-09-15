package com.example.muslimvn.presentation.viewmodels

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.muslimvn.data.local.entities.DownloadedVideoEntity
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.VideoDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class DownloadedVideosViewModel @Inject constructor(
    private val downloadManager: VideoDownloadManager,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    val downloadedVideos: StateFlow<List<DownloadedVideoEntity>> = downloadManager.allDownloadedVideos
        .map { list ->
            list.filter {
                it.mediaType != "AUDIO" &&
                    !it.localFilePath.endsWith(".mp3") &&
                    !it.localFilePath.endsWith(".m4a")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloadedAudios: StateFlow<List<DownloadedVideoEntity>> = downloadManager.allDownloadedVideos
        .map { list ->
            list.filter {
                it.mediaType == "AUDIO" ||
                    it.localFilePath.endsWith(".mp3") ||
                    it.localFilePath.endsWith(".m4a")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun playAudio(entity: DownloadedVideoEntity) {
        audioPlayerManager.playPodcast(
            url = entity.localFilePath,
            mediaId = entity.id,
            title = entity.title,
            artist = entity.uploaderName,
            artworkPath = entity.thumbnailUrl
        )
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownloadedVideo(videoId)
        }
    }
}
