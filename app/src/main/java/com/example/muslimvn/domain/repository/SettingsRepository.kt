package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.PrayerAdjustments
import com.example.muslimvn.domain.models.PrayerReminder
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getPrayerReminders(): Flow<Map<String, PrayerReminder>>
    suspend fun updateReminder(reminder: PrayerReminder)
    
    fun getAppTheme(): Flow<AppTheme>
    suspend fun updateAppTheme(theme: AppTheme)
    
    fun getCalculationMethod(): Flow<String>
    suspend fun updateCalculationMethod(method: String)

    fun getAsrMethod(): Flow<AsrMethod>
    suspend fun updateAsrMethod(method: AsrMethod)

    fun getPrayerAdjustments(): Flow<PrayerAdjustments>
    suspend fun updatePrayerAdjustments(adjustments: PrayerAdjustments)

    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
}
