package com.example.muslimvn.domain.repository

import com.example.muslimvn.data.local.entities.TrackerEntity
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    fun getTrackerByDateFlow(date: String): Flow<TrackerEntity?>
    suspend fun getTrackerByDate(date: String): TrackerEntity?
    suspend fun saveTracker(tracker: TrackerEntity)
    suspend fun updateQuranProgress(date: String, surahName: String, ayahNumber: Int, progress: Float)
}
