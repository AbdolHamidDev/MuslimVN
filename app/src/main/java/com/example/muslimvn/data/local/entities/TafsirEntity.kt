package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tafsirs")
data class TafsirEntity(
    @PrimaryKey val verseKey: String, // format "1:1"
    val resourceId: Int,
    val text: String,
    val translatedText: String? = null // For future use when we support translation
)
