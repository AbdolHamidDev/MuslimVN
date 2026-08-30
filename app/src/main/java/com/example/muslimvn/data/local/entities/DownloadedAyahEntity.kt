package com.example.muslimvn.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "downloaded_ayahs",
    primaryKeys = ["verseKey", "reciterId"]
)
data class DownloadedAyahEntity(
    val verseKey: String, // format "1:1"
    val reciterId: Int,
    val localPath: String
)
