package com.example.muslimvn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.muslimvn.data.local.entities.HijriDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HijriCalendarDao {

    @Query(
        "SELECT * FROM hijri_days " +
            "WHERE gregorianMonth = :month AND gregorianYear = :year " +
            "ORDER BY gregorianDate ASC"
    )
    fun observeGregorianMonth(month: Int, year: Int): Flow<List<HijriDayEntity>>

    @Query(
        "SELECT * FROM hijri_days " +
            "WHERE gregorianMonth = :month AND gregorianYear = :year " +
            "ORDER BY gregorianDate ASC"
    )
    suspend fun getGregorianMonth(month: Int, year: Int): List<HijriDayEntity>

    @Query(
        "SELECT MAX(cachedAtEpochMillis) FROM hijri_days " +
            "WHERE gregorianMonth = :month AND gregorianYear = :year"
    )
    suspend fun getLastCachedAt(month: Int, year: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<HijriDayEntity>)
}