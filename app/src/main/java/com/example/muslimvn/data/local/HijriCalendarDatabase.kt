package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.muslimvn.data.local.dao.HijriCalendarDao
import com.example.muslimvn.data.local.entities.HijriDayEntity

@Database(entities = [HijriDayEntity::class], version = 1, exportSchema = false)
abstract class HijriCalendarDatabase : RoomDatabase() {
    abstract val hijriCalendarDao: HijriCalendarDao

    companion object {
        const val DATABASE_NAME = "hijri_calendar_db"
    }
}