package com.example.muslimvn.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.muslimvn.core.di.QuranDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class QuranDisplayMode {
    BOTH, ARABIC_ONLY, TRANSLATION_ONLY
}

@Singleton
class QuranPreferences @Inject constructor(
    @QuranDataStore private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val RECITER_IDENTIFIER = stringPreferencesKey("reciter_identifier")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
    }

    val reciterIdentifier: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[Keys.RECITER_IDENTIFIER] ?: "Alafasy_128kbps"
        }

    val fontSize: Flow<Float> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[Keys.FONT_SIZE] ?: 18f
        }

    val displayMode: Flow<QuranDisplayMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            val modeName = preferences[Keys.DISPLAY_MODE] ?: QuranDisplayMode.BOTH.name
            try {
                QuranDisplayMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                QuranDisplayMode.BOTH
            }
        }

    suspend fun saveReciterIdentifier(identifier: String) {
        dataStore.edit { preferences ->
            preferences[Keys.RECITER_IDENTIFIER] = identifier
        }
    }

    suspend fun saveFontSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.FONT_SIZE] = size
        }
    }

    suspend fun saveDisplayMode(mode: QuranDisplayMode) {
        dataStore.edit { preferences ->
            preferences[Keys.DISPLAY_MODE] = mode.name
        }
    }
}
