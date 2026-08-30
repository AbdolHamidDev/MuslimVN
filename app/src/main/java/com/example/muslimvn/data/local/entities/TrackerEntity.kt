package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracker_data")
data class TrackerEntity(
    @PrimaryKey val date: String, // Định dạng yyyy-MM-dd
    
    // Fajr
    val fajrCompleted: Boolean = false,
    val fajrJamaah: Boolean = false,
    val fajrSunnahBefore: Boolean = false,
    
    // Dhuhr
    val dhuhrCompleted: Boolean = false,
    val dhuhrJamaah: Boolean = false,
    val dhuhrSunnahBefore: Boolean = false,
    val dhuhrSunnahAfter: Boolean = false,
    
    // Asr
    val asrCompleted: Boolean = false,
    val asrJamaah: Boolean = false,
    val asrSunnahBefore: Boolean = false,
    
    // Maghrib
    val maghribCompleted: Boolean = false,
    val maghribJamaah: Boolean = false,
    val maghribSunnahAfter: Boolean = false,
    
    // Isha
    val ishaCompleted: Boolean = false,
    val ishaJamaah: Boolean = false,
    val ishaSunnahAfter: Boolean = false,
    
    // Quran
    val lastSurahName: String = "Al-Fatihah",
    val lastSurahNumber: Int = 1,
    val lastAyahNumber: Int = 1,
    val quranProgress: Float = 0f,
    
    // Azkar
    val azkarCount: Int = 0
)
