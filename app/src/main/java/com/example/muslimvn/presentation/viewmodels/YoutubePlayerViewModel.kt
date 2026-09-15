package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.YouTubePlayerManager
import com.example.muslimvn.data.util.VideoDownloadManager
import com.example.muslimvn.data.util.DownloadStatus
import com.example.muslimvn.core.utils.TimeUtils
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.models.YoutubeVideoDetail
import com.example.muslimvn.domain.repository.YoutubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YoutubePlayerUiState(
    val videoDetail: YoutubeVideoDetail? = null,
    val suggestedVideos: List<YoutubeVideo> = emptyList(),
    val channelName: String = "",
    val isLoadingDetail: Boolean = false,
    val detailError: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle
)

@HiltViewModel
class YoutubePlayerViewModel @Inject constructor(
    val playerManager: YouTubePlayerManager,
    private val youtubeRepository: YoutubeRepository,
    private val downloadManager: VideoDownloadManager,
    private val downloadedVideoDao: com.example.muslimvn.data.local.dao.DownloadedVideoDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialVideoUrl: String = checkNotNull(savedStateHandle["videoUrl"])
    private val initialVideoTitle: String = savedStateHandle["videoTitle"] ?: ""
    private val initialChannelName: String = savedStateHandle["channelName"] ?: ""
    private val channelUrl: String = savedStateHandle["channelUrl"] ?: ""

    private val _uiState = MutableStateFlow(YoutubePlayerUiState(channelName = initialChannelName))
    val uiState = _uiState.asStateFlow()

    private var currentVideoId: String = initialVideoUrl.substringAfter("v=", "").substringBefore("&").ifEmpty { "vid_${initialVideoUrl.hashCode()}" }

    init {
        playerManager.playVideo(initialVideoUrl, initialVideoTitle, channelName = initialChannelName)
        if (initialVideoUrl.startsWith("file://") || initialVideoUrl.startsWith("/")) {
            viewModelScope.launch {
                val allVideos = downloadedVideoDao.getAllDownloadedVideosOnce()
                val found = allVideos.find { 
                    it.localFilePath == initialVideoUrl || 
                    it.localFilePath == android.net.Uri.parse(initialVideoUrl).path ||
                    initialVideoUrl.contains(it.id)
                }
                if (found != null) {
                    currentVideoId = found.id
                }
                observeDownloadStatus(currentVideoId)
            }
        } else {
            fetchVideoDetail(initialVideoUrl)
            fetchSuggestedVideos()
            observeDownloadStatus(currentVideoId)
        }
    }

    private fun observeDownloadStatus(videoId: String) {
        viewModelScope.launch {
            downloadManager.getStatusFlow(videoId).collectLatest { status ->
                _uiState.update { it.copy(downloadStatus = status) }
            }
        }
    }

    fun selectVideo(videoUrl: String, videoTitle: String) {
        playerManager.playVideo(videoUrl, videoTitle, channelName = _uiState.value.channelName)
        currentVideoId = videoUrl.substringAfter("v=", "").substringBefore("&").ifEmpty { "vid_${videoUrl.hashCode()}" }
        if (!videoUrl.startsWith("file://") && !videoUrl.startsWith("/")) {
            fetchVideoDetail(videoUrl)
        }
        observeDownloadStatus(currentVideoId)
    }

    fun toggleDownload() {
        val detail = _uiState.value.videoDetail
        val videoUrl = detail?.videoUrl ?: playerManager.currentVideoUrl.value ?: initialVideoUrl
        val title = detail?.title ?: playerManager.videoTitle.value
        val thumbnailUrl = detail?.thumbnailUrl ?: ""
        val uploaderName = detail?.uploaderName ?: _uiState.value.channelName
        val duration = ""

        when (val status = _uiState.value.downloadStatus) {
            is DownloadStatus.Idle, is DownloadStatus.Failed -> {
                downloadManager.startDownload(currentVideoId, videoUrl, title, thumbnailUrl, uploaderName, duration)
            }
            is DownloadStatus.Downloading -> {
                downloadManager.pauseDownload(currentVideoId)
            }
            is DownloadStatus.Paused -> {
                downloadManager.resumeDownload(currentVideoId, videoUrl, title, thumbnailUrl, uploaderName, duration)
            }
            is DownloadStatus.Completed -> {
                // Đã tải xong, có thể hỏi xóa hoặc không làm gì
            }
        }
    }

    private fun fetchVideoDetail(videoUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, detailError = null) }
            youtubeRepository.getVideoDetail(videoUrl).collect { result ->
                result.onSuccess { detail ->
                    _uiState.update { 
                        it.copy(
                            videoDetail = detail,
                            channelName = detail.uploaderName,
                            isLoadingDetail = false
                        )
                    }
                    // Cập nhật lại player với thông tin metadata đầy đủ hơn nếu cần
                    playerManager.playVideo(
                        videoUrl = detail.videoUrl,
                        title = detail.title,
                        thumbnailUrl = detail.thumbnailUrl,
                        channelName = detail.uploaderName
                    )
                }.onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoadingDetail = false,
                            detailError = e.message
                        )
                    }
                    // Fallback to channel videos if detail fetch fails
                    fetchSuggestedVideos()
                }
            }
        }
    }

    private fun fetchSuggestedVideos() {
        viewModelScope.launch {
            youtubeRepository.getVideosByChannel(channelUrl).collect { result ->
                result.onSuccess { videos ->
                    _uiState.update { it.copy(suggestedVideos = videos) }
                }
            }
        }
    }
}
