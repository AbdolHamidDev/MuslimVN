package com.example.muslimvn.domain.models

data class VerseTiming(
    val verseKey: String,
    val segments: List<WordSegment>
)

data class WordSegment(
    val wordIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long
)
