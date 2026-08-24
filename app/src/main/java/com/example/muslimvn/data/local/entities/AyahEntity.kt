package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ayahs",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["number"],
            childColumns = ["surahId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["surahId"])]
)
data class AyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surahId: Int,
    val ayahNumber: Int,
    val textArabic: String,
    val textVietnamese: String,
    val isBookmarked: Boolean = false
)
