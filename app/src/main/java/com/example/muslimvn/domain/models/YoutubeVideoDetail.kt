package com.example.muslimvn.domain.models

data class YoutubeVideoDetail(
    val id: String,
    val title: String,
    val description: String,
    val uploadDateTimestamp: Long, // Epoch millis
    val viewCount: Long,
    val videoUrl: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String,
    val uploaderAvatarUrl: String,
    val relatedVideos: List<YoutubeVideo>
)
