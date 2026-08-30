package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.repository.TrackerRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class UpdateTrackerQuranUseCase @Inject constructor(
    private val repository: TrackerRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend operator fun invoke(surahName: String, surahNumber: Int, ayahNumber: Int, progress: Float = 0f) {
        val dateKey = dateFormatter.format(Date())
        val currentTracker = repository.getTrackerByDate(dateKey)
            ?: com.example.muslimvn.data.local.entities.TrackerEntity(date = dateKey)
        
        val updatedTracker = currentTracker.copy(
            lastSurahName = surahName,
            lastSurahNumber = surahNumber,
            lastAyahNumber = ayahNumber,
            quranProgress = progress
        )
        repository.saveTracker(updatedTracker)
    }
}
