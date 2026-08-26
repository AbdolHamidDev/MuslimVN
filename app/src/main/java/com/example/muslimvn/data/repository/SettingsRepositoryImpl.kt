package com.example.muslimvn.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.muslimvn.core.di.SettingsDataStore
import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.ReminderMode
import com.example.muslimvn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun getPrayerReminders(): Flow<Map<String, PrayerReminder>> {
        return dataStore.data.map { preferences ->
            PRAYER_TYPES.associateWith { type ->
                val modeStr = preferences[stringPreferencesKey("reminder_mode_$type")]
                val adhanFile = preferences[stringPreferencesKey("reminder_adhan_$type")] ?: "Mishary-Alafasi.mp3"
                
                val mode = try {
                    if (modeStr != null) ReminderMode.valueOf(modeStr) else ReminderMode.NOTIFICATION
                } catch (e: Exception) {
                    ReminderMode.NOTIFICATION
                }

                PrayerReminder(
                    prayerType = type,
                    mode = mode,
                    adhanFileName = adhanFile
                )
            }
        }
    }

    override suspend fun updateReminder(reminder: PrayerReminder) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("reminder_mode_${reminder.prayerType}")] = reminder.mode.name
            reminder.adhanFileName?.let {
                preferences[stringPreferencesKey("reminder_adhan_${reminder.prayerType}")] = it
            }
        }
    }

    override fun getAppTheme(): Flow<AppTheme> {
        return dataStore.data.map { preferences ->
            val themeStr = preferences[KEY_APP_THEME]
            try {
                if (themeStr != null) AppTheme.valueOf(themeStr) else AppTheme.FOLLOW_SYSTEM
            } catch (e: Exception) {
                AppTheme.FOLLOW_SYSTEM
            }
        }
    }

    override suspend fun updateAppTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_THEME] = theme.name
        }
    }

    override fun getCalculationMethod(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[KEY_CALCULATION_METHOD] ?: "MUSLIM_WORLD_LEAGUE"
        }
    }

    override suspend fun updateCalculationMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CALCULATION_METHOD] = method
        }
    }

    companion object {
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        private val PRAYER_TYPES = listOf(
            PrayerName.FAJR,
            PrayerName.SUNRISE,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA
        )
    }
}
