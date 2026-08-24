package com.example.muslimvn.domain.models

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameVietnamese: String,
    val totalAyahs: Int,
    val revelationType: String
)
