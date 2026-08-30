package com.example.muslimvn.data.local.dao

import androidx.room.*
import com.example.muslimvn.data.local.entities.TrackerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM tracker_data WHERE date = :date")
    suspend fun getTrackerByDate(date: String): TrackerEntity?

    @Query("SELECT * FROM tracker_data WHERE date = :date")
    fun getTrackerByDateFlow(date: String): Flow<TrackerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: TrackerEntity)

    @Query("UPDATE tracker_data SET lastSurahName = :surahName, lastAyahNumber = :ayahNumber, quranProgress = :progress WHERE date = :date")
    suspend fun updateQuranProgress(date: String, surahName: String, ayahNumber: Int, progress: Float)
}
