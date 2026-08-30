package com.example.muslimvn.domain.models

data class VerseTiming(
    val verseKey: String,
    val segments: List<WordSegment>,
    val audioUrl: String? = null
)

data class WordSegment(
    val wordIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long
)
