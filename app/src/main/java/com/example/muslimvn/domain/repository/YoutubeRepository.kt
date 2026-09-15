package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.models.YoutubeVideoDetail
import kotlinx.coroutines.flow.Flow

interface YoutubeRepository {
    fun getVideosByChannel(channelUrl: String): Flow<Result<List<YoutubeVideo>>>
    suspend fun loadMoreVideos(channelUrl: String): Result<List<YoutubeVideo>>
    fun getVideoStreamUrl(videoUrl: String): Flow<Result<String>>
    fun getVideoDetail(videoUrl: String): Flow<Result<YoutubeVideoDetail>>
}
