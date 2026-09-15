package com.example.muslimvn.domain.models

data class YoutubeVideo(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val duration: Long,
    val viewCount: Long,
    val uploadDate: String,
    val videoUrl: String
)
