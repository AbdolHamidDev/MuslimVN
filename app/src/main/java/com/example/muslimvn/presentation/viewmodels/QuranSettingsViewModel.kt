package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.data.preferences.QuranPreferences
import com.example.muslimvn.domain.models.Reciter
import com.example.muslimvn.domain.models.availableReciters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranSettingsViewModel @Inject constructor(
    private val preferences: QuranPreferences
) : ViewModel() {

    val reciterIdentifier: StateFlow<String> = preferences.reciterIdentifier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alafasy_128kbps")

    val fontSize: StateFlow<Float> = preferences.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)

    val displayMode: StateFlow<QuranDisplayMode> = preferences.displayMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranDisplayMode.BOTH)

    val hapticEnabled: StateFlow<Boolean> = preferences.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onReciterSelected(reciter: Reciter) {
        viewModelScope.launch {
            preferences.saveReciterIdentifier(reciter.identifier)
        }
    }

    fun onFontSizeChanged(size: Float) {
        viewModelScope.launch {
            preferences.saveFontSize(size)
        }
    }

    fun onDisplayModeChanged(mode: QuranDisplayMode) {
        viewModelScope.launch {
            preferences.saveDisplayMode(mode)
        }
    }

    fun onHapticEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveHapticEnabled(enabled)
        }
    }
}
