package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.ReminderMode
import com.example.muslimvn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrayerNotificationsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val reminders: StateFlow<Map<String, PrayerReminder>> = repository.getPrayerReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val calculationMethod: StateFlow<String> = repository.getCalculationMethod()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "MUSLIM_WORLD_LEAGUE"
        )

    fun onReminderModeChanged(prayerType: String, mode: ReminderMode) {
        val currentReminder = reminders.value[prayerType] ?: PrayerReminder(prayerType)
        viewModelScope.launch {
            repository.updateReminder(currentReminder.copy(mode = mode))
        }
    }

    fun onAdhanFileChanged(prayerType: String, fileName: String) {
        val currentReminder = reminders.value[prayerType] ?: PrayerReminder(prayerType)
        viewModelScope.launch {
            repository.updateReminder(currentReminder.copy(adhanFileName = fileName))
        }
    }

    fun onCalculationMethodChanged(method: String) {
        viewModelScope.launch {
            repository.updateCalculationMethod(method)
        }
    }
}
