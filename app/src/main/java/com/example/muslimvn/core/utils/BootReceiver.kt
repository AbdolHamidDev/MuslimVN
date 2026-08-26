package com.example.muslimvn.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.muslimvn.domain.repository.SettingsRepository
import com.example.muslimvn.domain.usecases.GetPrayerTimesUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase

    @Inject
    lateinit var adhanScheduler: AdhanScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val prayerTimes = getPrayerTimesUseCase()
                    val reminders = settingsRepository.getPrayerReminders().first()
                    adhanScheduler.scheduleNextWithSettings(prayerTimes, reminders)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
