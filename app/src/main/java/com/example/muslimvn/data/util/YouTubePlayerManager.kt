package com.example.muslimvn.data.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.muslimvn.domain.repository.YoutubeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class YouTubePlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youtubeRepository: YoutubeRepository
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        playWhenReady = true
    }

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    private val _videoTitle = MutableStateFlow("")
    val videoTitle: StateFlow<String> = _videoTitle.asStateFlow()

    private val _thumbnailUrl = MutableStateFlow("")
    val thumbnailUrl: StateFlow<String> = _thumbnailUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Tỉ lệ khung hình thật của stream. Mặc định 16:9 trong lúc chưa có metadata. */
    private val _videoAspectRatio = MutableStateFlow(DEFAULT_VIDEO_ASPECT_RATIO)
    val videoAspectRatio: StateFlow<Float> = _videoAspectRatio.asStateFlow()

    private val _isPortraitVideo = MutableStateFlow(false)
    val isPortraitVideo: StateFlow<Boolean> = _isPortraitVideo.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val width = videoSize.width
                val height = videoSize.height
                if (width <= 0 || height <= 0) return

                val aspectRatio = (width.toFloat() * videoSize.pixelWidthHeightRatio) / height
                _videoAspectRatio.value = aspectRatio
                // Dùng ngưỡng thay vì so sánh chính xác để vẫn nhận diện đúng 9:16
                // khi stream có pixel aspect ratio không phải 1:1.
                _isPortraitVideo.value = aspectRatio < 1f
            }
        })
        mediaSession?.release()
        mediaSession = MediaSession.Builder(context, exoPlayer)
            .setId("YouTubePlayerManagerSession")
            .build()
    }

    fun playVideo(videoUrl: String, title: String, thumbnailUrl: String = "", channelName: String = "") {
        val isSameVideo = _currentVideoUrl.value == videoUrl && exoPlayer.playbackState != Player.STATE_IDLE
        
        if (isSameVideo) {
            // Cập nhật metadata nếu cần
            if (title != _videoTitle.value || channelName.isNotEmpty()) {
                val metadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(channelName)
                    .setArtworkUri(if (thumbnailUrl.isNotEmpty()) android.net.Uri.parse(thumbnailUrl) else null)
                    .build()
                exoPlayer.replaceMediaItem(exoPlayer.currentMediaItemIndex, exoPlayer.currentMediaItem!!.buildUpon().setMediaMetadata(metadata).build())
            }
            return
        }

        _currentVideoUrl.value = videoUrl
        _videoTitle.value = title
        _thumbnailUrl.value = thumbnailUrl
        _isLoading.value = true
        _error.value = null
        // Tránh giữ tỉ lệ của video trước trong lúc stream mới đang tải.
        _videoAspectRatio.value = DEFAULT_VIDEO_ASPECT_RATIO
        _isPortraitVideo.value = false

        val isLocalFile = videoUrl.startsWith("file://") || videoUrl.startsWith("/")

        if (isLocalFile) {
            _isLoading.value = false
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(channelName)
                .setArtworkUri(if (thumbnailUrl.isNotEmpty()) android.net.Uri.parse(thumbnailUrl) else null)
                .build()
            val mediaItem = MediaItem.Builder()
                .setMediaId(videoUrl)
                .setUri(android.net.Uri.parse(videoUrl))
                .setMediaMetadata(metadata)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            scope.launch {
                youtubeRepository.getVideoStreamUrl(videoUrl).collect { result ->
                    result.onSuccess { streamUrl ->
                        _isLoading.value = false
                        val metadata = MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(channelName)
                            .setArtworkUri(if (thumbnailUrl.isNotEmpty()) android.net.Uri.parse(thumbnailUrl) else null)
                            .build()
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(videoUrl)
                            .setUri(streamUrl)
                            .setMediaMetadata(metadata)
                            .build()
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }.onFailure { e ->
                        _isLoading.value = false
                        _error.value = e.message
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun stop() {
        exoPlayer.stop()
        _currentVideoUrl.value = null
    }

    fun getMediaSession(): MediaSession? = mediaSession

    private companion object {
        const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f
    }
}
