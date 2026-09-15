package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.models.YoutubeVideoDetail
import kotlinx.coroutines.flow.Flow

data class StreamDownloadInfo(
    val url: String,
    val sizeBytes: Long
)

data class VideoAndAudioStreamInfo(
    val videoInfo: StreamDownloadInfo?,
    val audioInfo: StreamDownloadInfo?
)

interface YoutubeRepository {
    fun getVideosByChannel(channelUrl: String): Flow<Result<List<YoutubeVideo>>>
    suspend fun loadMoreVideos(channelUrl: String): Result<List<YoutubeVideo>>
    fun getVideoStreamUrl(videoUrl: String): Flow<Result<String>>
    fun getAudioStreamUrl(videoUrl: String): Flow<Result<String>>
    fun getMediaStreamInfo(videoUrl: String): Flow<Result<VideoAndAudioStreamInfo>>
    fun getVideoDetail(videoUrl: String): Flow<Result<YoutubeVideoDetail>>
}
