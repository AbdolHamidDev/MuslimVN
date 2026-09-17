package com.example.muslimvn.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.muslimvn.core.di.SettingsDataStore
import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.PrayerAdjustments
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
            preferences[KEY_CALCULATION_METHOD] ?: "MUSLIMVN_DEFAULT"
        }
    }

    override suspend fun updateCalculationMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CALCULATION_METHOD] = method
        }
    }

    override fun getAsrMethod(): Flow<AsrMethod> {
        return dataStore.data.map { preferences ->
            val asrStr = preferences[KEY_ASR_METHOD]
            try {
                if (asrStr != null) AsrMethod.valueOf(asrStr) else AsrMethod.STANDARD
            } catch (e: Exception) {
                AsrMethod.STANDARD
            }
        }
    }

    override suspend fun updateAsrMethod(method: AsrMethod) {
        dataStore.edit { preferences ->
            preferences[KEY_ASR_METHOD] = method.name
        }
    }

    override fun getPrayerAdjustments(): Flow<PrayerAdjustments> {
        return dataStore.data.map { preferences ->
            PrayerAdjustments(
                fajr = preferences[KEY_ADJUST_FAJR] ?: 0,
                dhuhr = preferences[KEY_ADJUST_DHUHR] ?: 0,
                asr = preferences[KEY_ADJUST_ASR] ?: 0,
                maghrib = preferences[KEY_ADJUST_MAGHRIB] ?: 0,
                isha = preferences[KEY_ADJUST_ISHA] ?: 0
            )
        }
    }

    override suspend fun updatePrayerAdjustments(adjustments: PrayerAdjustments) {
        dataStore.edit { preferences ->
            preferences[KEY_ADJUST_FAJR] = adjustments.fajr
            preferences[KEY_ADJUST_DHUHR] = adjustments.dhuhr
            preferences[KEY_ADJUST_ASR] = adjustments.asr
            preferences[KEY_ADJUST_MAGHRIB] = adjustments.maghrib
            preferences[KEY_ADJUST_ISHA] = adjustments.isha
        }
    }

    companion object {
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        private val KEY_ASR_METHOD = stringPreferencesKey("asr_method")
        private val KEY_ADJUST_FAJR = intPreferencesKey("adjust_fajr")
        private val KEY_ADJUST_DHUHR = intPreferencesKey("adjust_dhuhr")
        private val KEY_ADJUST_ASR = intPreferencesKey("adjust_asr")
        private val KEY_ADJUST_MAGHRIB = intPreferencesKey("adjust_maghrib")
        private val KEY_ADJUST_ISHA = intPreferencesKey("adjust_isha")

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
