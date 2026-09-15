package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val localFilePath: String,
    val duration: String,
    val downloadDate: Long,
    val fileSize: Long
)
