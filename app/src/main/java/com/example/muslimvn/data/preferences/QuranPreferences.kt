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

enum class QuranViewMode {
    LIST, MUSHAF
}

@Singleton
class QuranPreferences @Inject constructor(
    @QuranDataStore private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val RECITER_IDENTIFIER = stringPreferencesKey("reciter_identifier")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val HAS_SELECTED_RECITER = booleanPreferencesKey("has_selected_reciter")
        fun downloadStatusKey(reciterId: Int) = stringPreferencesKey("download_status_$reciterId")
    }

    val reciterIdentifier: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[Keys.RECITER_IDENTIFIER] ?: "Alafasy_128kbps"
        }

    val hasSelectedReciter: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[Keys.HAS_SELECTED_RECITER] ?: false
        }

    val fontSize: Flow<Float> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[Keys.FONT_SIZE] ?: 24f
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

    val viewMode: Flow<QuranViewMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            val modeName = preferences[Keys.VIEW_MODE] ?: QuranViewMode.LIST.name
            try {
                QuranViewMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                QuranViewMode.LIST
            }
        }

    fun getDownloadStatus(reciterId: Int): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[Keys.downloadStatusKey(reciterId)] ?: "IDLE"
        }

    suspend fun saveDownloadStatus(reciterId: Int, status: String) {
        dataStore.edit { preferences ->
            preferences[Keys.downloadStatusKey(reciterId)] = status
        }
    }

    suspend fun saveReciterIdentifier(identifier: String) {
        dataStore.edit { preferences ->
            preferences[Keys.RECITER_IDENTIFIER] = identifier
            preferences[Keys.HAS_SELECTED_RECITER] = true
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

    suspend fun saveViewMode(mode: QuranViewMode) {
        dataStore.edit { preferences ->
            preferences[Keys.VIEW_MODE] = mode.name
        }
    }
}
