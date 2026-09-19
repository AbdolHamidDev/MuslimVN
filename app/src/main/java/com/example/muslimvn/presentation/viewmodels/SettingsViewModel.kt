package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.AppTheme
import com.example.muslimvn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = repository.getAppTheme()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.FOLLOW_SYSTEM
        )

    val useDynamicColor: StateFlow<Boolean> = repository.useDynamicColor()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateAppTheme(theme)
        }
    }

    fun onDynamicColorChanged(useDynamicColor: Boolean) {
        viewModelScope.launch {
            repository.updateUseDynamicColor(useDynamicColor)
        }
    }
}
