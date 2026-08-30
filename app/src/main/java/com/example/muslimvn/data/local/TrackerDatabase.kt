package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.muslimvn.data.local.dao.TrackerDao
import com.example.muslimvn.data.local.entities.TrackerEntity

@Database(entities = [TrackerEntity::class], version = 2, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract val trackerDao: TrackerDao

    companion object {
        const val DATABASE_NAME = "tracker_db"
    }
}
