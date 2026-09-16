package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.YouTubePlayerManager
import com.example.muslimvn.data.util.VideoDownloadManager
import com.example.muslimvn.data.util.DownloadMediaType
import com.example.muslimvn.data.util.DownloadStatus
import com.example.muslimvn.core.utils.TimeUtils
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.models.YoutubeVideoDetail
import com.example.muslimvn.domain.repository.YoutubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class YoutubePlayerUiState(
    val videoDetail: YoutubeVideoDetail? = null,
    val suggestedVideos: List<YoutubeVideo> = emptyList(),
    val channelName: String = "",
    val isLoadingDetail: Boolean = false,
    val detailError: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
    val videoDownloadStatus: DownloadStatus = DownloadStatus.Idle,
    val audioDownloadStatus: DownloadStatus = DownloadStatus.Idle,
    val showDownloadDialog: Boolean = false,
    val videoSizeBytes: Long = 0L,
    val audioSizeBytes: Long = 0L,
    val isLoadingStreamInfo: Boolean = false
)

@HiltViewModel(assistedFactory = YoutubePlayerViewModel.Factory::class)
class YoutubePlayerViewModel @AssistedInject constructor(
    val playerManager: YouTubePlayerManager,
    private val youtubeRepository: YoutubeRepository,
    private val downloadManager: VideoDownloadManager,
    private val downloadedVideoDao: com.example.muslimvn.data.local.dao.DownloadedVideoDao,
    @Assisted("videoUrl") private val initialVideoUrl: String,
    @Assisted("videoTitle") private val initialVideoTitle: String,
    @Assisted("channelName") private val initialChannelName: String,
    @Assisted("channelUrl") private val channelUrl: String
) : ViewModel() {
    @AssistedFactory
    interface Factory { fun create(@Assisted("videoUrl") videoUrl: String, @Assisted("videoTitle") videoTitle: String, @Assisted("channelName") channelName: String, @Assisted("channelUrl") channelUrl: String): YoutubePlayerViewModel }

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
                    currentVideoId = found.videoId.ifEmpty { found.id.substringBefore("_") }
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
            downloadManager.getVideoAndAudioStatuses(videoId).collectLatest { (videoStat, audioStat) ->
                val combinedStatus = when {
                    videoStat is DownloadStatus.Downloading -> videoStat
                    audioStat is DownloadStatus.Downloading -> audioStat
                    videoStat is DownloadStatus.Completed || audioStat is DownloadStatus.Completed -> DownloadStatus.Completed
                    else -> DownloadStatus.Idle
                }
                _uiState.update { 
                    it.copy(
                        downloadStatus = combinedStatus,
                        videoDownloadStatus = videoStat,
                        audioDownloadStatus = audioStat
                    ) 
                }
            }
        }
    }

    fun selectVideo(videoUrl: String, videoTitle: String) {
        playerManager.playVideo(videoUrl, videoTitle, channelName = _uiState.value.channelName)
        currentVideoId = videoUrl.substringAfter("v=", "").substringBefore("&").ifEmpty { "vid_${videoUrl.hashCode()}" }
        _uiState.update { 
            it.copy(
                videoSizeBytes = 0L,
                audioSizeBytes = 0L,
                showDownloadDialog = false
            ) 
        }
        if (!videoUrl.startsWith("file://") && !videoUrl.startsWith("/")) {
            fetchVideoDetail(videoUrl)
        }
        observeDownloadStatus(currentVideoId)
    }

    fun onDownloadClick() {
        _uiState.update { it.copy(showDownloadDialog = true) }
        if (_uiState.value.videoSizeBytes == 0L && _uiState.value.audioSizeBytes == 0L) {
            fetchStreamInfo()
        }
    }

    fun dismissDownloadDialog() {
        _uiState.update { it.copy(showDownloadDialog = false) }
    }

    private fun fetchStreamInfo() {
        val detail = _uiState.value.videoDetail
        val videoUrl = detail?.videoUrl ?: playerManager.currentVideoUrl.value ?: initialVideoUrl
        if (videoUrl.startsWith("file://") || videoUrl.startsWith("/")) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStreamInfo = true) }
            youtubeRepository.getMediaStreamInfo(videoUrl).collect { result ->
                result.onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            videoSizeBytes = info.videoInfo?.sizeBytes ?: 0L,
                            audioSizeBytes = info.audioInfo?.sizeBytes ?: 0L,
                            isLoadingStreamInfo = false
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoadingStreamInfo = false) }
                }
            }
        }
    }

    fun startDownloadVideo() {
        startDownloadInternal(DownloadMediaType.VIDEO)
    }

    fun startDownloadAudio() {
        startDownloadInternal(DownloadMediaType.AUDIO)
    }

    private fun startDownloadInternal(mediaType: DownloadMediaType) {
        val detail = _uiState.value.videoDetail
        val videoUrl = detail?.videoUrl ?: playerManager.currentVideoUrl.value ?: initialVideoUrl
        val title = detail?.title ?: playerManager.videoTitle.value
        val thumbnailUrl = detail?.thumbnailUrl ?: ""
        val uploaderName = detail?.uploaderName ?: _uiState.value.channelName
        val duration = ""

        downloadManager.startDownload(
            videoId = currentVideoId,
            videoUrl = videoUrl,
            title = title,
            thumbnailUrl = thumbnailUrl,
            uploaderName = uploaderName,
            duration = duration,
            mediaType = mediaType
        )
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
