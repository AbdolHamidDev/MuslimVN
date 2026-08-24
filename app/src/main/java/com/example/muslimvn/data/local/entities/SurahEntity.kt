package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val nameArabic: String,
    val nameVietnamese: String,
    val totalAyahs: Int,
    val revelationType: String
)
