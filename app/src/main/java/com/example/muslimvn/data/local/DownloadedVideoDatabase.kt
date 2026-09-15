package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.muslimvn.data.local.dao.DownloadedVideoDao
import com.example.muslimvn.data.local.entities.DownloadedVideoEntity

@Database(
    entities = [DownloadedVideoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DownloadedVideoDatabase : RoomDatabase() {
    abstract val downloadedVideoDao: DownloadedVideoDao

    companion object {
        const val DATABASE_NAME = "downloaded_video_db"
    }
}
