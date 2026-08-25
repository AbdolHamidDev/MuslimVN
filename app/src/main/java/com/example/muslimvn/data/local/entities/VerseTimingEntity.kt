package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verse_timings")
data class VerseTimingEntity(
    @PrimaryKey val verseKey: String, // format "1:1"
    val reciterId: Int,
    val segmentsJson: String // Lưu danh sách WordSegment dưới dạng JSON cho gọn
)
