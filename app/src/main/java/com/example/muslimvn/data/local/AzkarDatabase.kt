package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.muslimvn.data.local.dao.AzkarDao
import com.example.muslimvn.data.local.entities.AzkarEntity

@Database(entities = [AzkarEntity::class], version = 4, exportSchema = false)
abstract class AzkarDatabase : RoomDatabase() {
    abstract val azkarDao: AzkarDao

    companion object {
        const val DATABASE_NAME = "azkar_db"
    }
}
