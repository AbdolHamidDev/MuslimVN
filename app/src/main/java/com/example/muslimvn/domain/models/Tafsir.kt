package com.example.muslimvn.domain.models

data class Tafsir(
    val verseKey: String,
    val resourceId: Int,
    val text: String,
    val translatedText: String? = null
)
