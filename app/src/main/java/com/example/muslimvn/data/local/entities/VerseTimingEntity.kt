package com.example.muslimvn.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "verse_timings",
    primaryKeys = ["verseKey", "reciterId"]
)
data class VerseTimingEntity(
    val verseKey: String, // format "1:1"
    val reciterId: Int,
    val segmentsJson: String, // Lưu danh sách WordSegment dưới dạng JSON cho gọn
    val audioUrl: String? = null
)
