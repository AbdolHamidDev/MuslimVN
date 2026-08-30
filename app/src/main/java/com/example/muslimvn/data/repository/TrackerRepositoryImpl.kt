package com.example.muslimvn.data.repository

import com.example.muslimvn.data.local.dao.TrackerDao
import com.example.muslimvn.data.local.entities.TrackerEntity
import com.example.muslimvn.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrackerRepositoryImpl @Inject constructor(
    private val dao: TrackerDao
) : TrackerRepository {
    override fun getTrackerByDateFlow(date: String): Flow<TrackerEntity?> =
        dao.getTrackerByDateFlow(date)

    override suspend fun getTrackerByDate(date: String): TrackerEntity? =
        dao.getTrackerByDate(date)

    override suspend fun saveTracker(tracker: TrackerEntity) =
        dao.insertTracker(tracker)

    override suspend fun updateQuranProgress(date: String, surahName: String, ayahNumber: Int, progress: Float) =
        dao.updateQuranProgress(date, surahName, ayahNumber, progress)
}
