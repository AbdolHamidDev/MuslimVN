package com.example.muslimvn.domain.models

data class Ayah(
    val id: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val textArabic: String,
    val textVietnamese: String,
    val isBookmarked: Boolean
)
