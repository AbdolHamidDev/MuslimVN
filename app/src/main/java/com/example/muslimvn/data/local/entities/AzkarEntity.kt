package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "azkar")
data class AzkarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String = "",
    val title: String = "",
    val contentArabic: String = "",
    val contentTransliteration: String = "",
    val contentVietnamese: String = "",
    val reference: String = "",
    val repeatCount: Int = 1,
    val isFavorite: Boolean = false
)
