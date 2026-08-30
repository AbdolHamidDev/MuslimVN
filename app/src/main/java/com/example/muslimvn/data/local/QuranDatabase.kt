package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.entities.AyahEntity
import com.example.muslimvn.data.local.entities.DownloadedAyahEntity
import com.example.muslimvn.data.local.entities.SurahEntity
import com.example.muslimvn.data.local.entities.TafsirEntity
import com.example.muslimvn.data.local.entities.VerseTimingEntity

@Database(entities = [SurahEntity::class, AyahEntity::class, VerseTimingEntity::class, TafsirEntity::class, DownloadedAyahEntity::class], version = 5, exportSchema = false)
abstract class QuranDatabase : RoomDatabase() {
    abstract val quranDao: QuranDao

    companion object {
        const val DATABASE_NAME = "quran_db"
    }
}
