package com.example.muslimvn.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.muslimvn.domain.repository.HijriCalendarRepository.Companion.DEFAULT_OFFSET_DAYS
import com.example.muslimvn.domain.repository.HijriCalendarRepository.Companion.MAX_OFFSET_DAYS
import com.example.muslimvn.domain.repository.HijriCalendarRepository.Companion.MIN_OFFSET_DAYS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed preferences for the Hijri calendar.
 *
 * `hijri_date_offset_days` lets the user shift rendered Hijri dates (-2..+2 days)
 * to match offshore moon-sighting reports accepted by the Vietnamese Muslim community.
 */
@Singleton
class HijriPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val offsetDays: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences()) // First run / IO error: fall back to defaults.
            } else {
                throw exception
            }
        }
        .map { preferences ->
            (preferences[KEY_OFFSET_DAYS] ?: DEFAULT_OFFSET_DAYS)
                .coerceIn(MIN_OFFSET_DAYS, MAX_OFFSET_DAYS)
        }

    suspend fun setOffsetDays(offset: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OFFSET_DAYS] = offset.coerceIn(MIN_OFFSET_DAYS, MAX_OFFSET_DAYS)
        }
    }

    companion object {
        val KEY_OFFSET_DAYS = intPreferencesKey("hijri_date_offset_days")
    }
}