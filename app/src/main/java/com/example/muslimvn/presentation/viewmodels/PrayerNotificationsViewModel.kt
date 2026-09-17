package com.example.muslimvn.presentation.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.PrayerAdjustments
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.ReminderMode
import com.example.muslimvn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrayerNotificationsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: SettingsRepository
) : ViewModel() {

    private val _isSystemNotificationEnabled = MutableStateFlow(true)
    val isSystemNotificationEnabled: StateFlow<Boolean> = _isSystemNotificationEnabled.asStateFlow()

    init {
        syncNotificationState()
    }

    fun syncNotificationState() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true 
        }
        _isSystemNotificationEnabled.update { granted }
    }

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
            initialValue = "MUSLIMVN_DEFAULT"
        )

    val asrMethod: StateFlow<AsrMethod> = repository.getAsrMethod()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AsrMethod.STANDARD
        )

    val prayerAdjustments: StateFlow<PrayerAdjustments> = repository.getPrayerAdjustments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PrayerAdjustments()
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

    fun onAsrMethodChanged(method: AsrMethod) {
        viewModelScope.launch {
            repository.updateAsrMethod(method)
        }
    }

    fun onPrayerAdjustmentsChanged(adjustments: PrayerAdjustments) {
        viewModelScope.launch {
            repository.updatePrayerAdjustments(adjustments)
        }
    }
}
