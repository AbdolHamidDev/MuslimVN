package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached Hijri calendar row for a single Gregorian date.
 *
 * [gregorianDate] follows the ISO-8601 `yyyy-MM-dd` pattern and is the primary key;
 * [gregorianMonth]/[gregorianYear] are denormalized to make cache lookups by month/year fast.
 */
@Entity(
    tableName = "hijri_days",
    indices = [Index("gregorianYear", "gregorianMonth")]
)
data class HijriDayEntity(
    @PrimaryKey val gregorianDate: String, // "yyyy-MM-dd"
    val hijriDay: Int,
    val hijriMonth: Int, // 1..12
    val hijriYear: Int,
    val dayOfWeek: Int, // ISO-8601 Monday=1 .. Sunday=7
    val eventsJson: String, // JSON array of holiday/event names from the API
    val gregorianMonth: Int, // 1..12
    val gregorianYear: Int,
    val cachedAtEpochMillis: Long
)