package com.example.muslimvn.presentation.viewmodels

import java.util.Date

data class PrayerTrackerState(
    val name: String,
    val time: Date = Date(),
    val isCompleted: Boolean = false,
    val isJamaah: Boolean = false,
    val hasSunnahBefore: Boolean = false,
    val hasSunnahAfter: Boolean = false,
    val isSunnahBeforeCompleted: Boolean = false,
    val isSunnahAfterCompleted: Boolean = false
)

data class DaySelection(
    val date: Date,
    val isSelected: Boolean = false,
    val completionProgress: Float = 0f
)

data class QuranTrackerState(
    val lastSurahName: String = "Al-Fatihah",
    val lastSurahNumber: Int = 1,
    val lastAyahNumber: Int = 1,
    val progress: Float = 0.05f
)

data class AzkarTrackerState(
    val count: Int = 0
)

data class TrackerUiState(
    val weeklyDays: List<DaySelection> = emptyList(),
    val prayers: List<PrayerTrackerState> = emptyList(),
    val quran: QuranTrackerState = QuranTrackerState(),
    val azkar: AzkarTrackerState = AzkarTrackerState(),
    val isLoading: Boolean = true
)
