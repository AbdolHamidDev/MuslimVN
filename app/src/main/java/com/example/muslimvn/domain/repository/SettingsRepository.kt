package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.models.PrayerReminder
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getPrayerReminders(): Flow<Map<String, PrayerReminder>>
    suspend fun updateReminder(reminder: PrayerReminder)
    
    fun getAppTheme(): Flow<AppTheme>
    suspend fun updateAppTheme(theme: AppTheme)
    
    fun getCalculationMethod(): Flow<String>
    suspend fun updateCalculationMethod(method: String)
}
