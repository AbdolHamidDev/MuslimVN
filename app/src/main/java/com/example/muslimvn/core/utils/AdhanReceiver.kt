package com.example.muslimvn.core.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.muslimvn.R
import com.example.muslimvn.core.di.NotificationModule
import com.example.muslimvn.domain.models.ReminderMode
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
class AdhanReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase

    @Inject
    lateinit var adhanScheduler: AdhanScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AdhanScheduler.ACTION_ADHAN_ALARM) return

        val prayerName =
            intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER_NAME) ?: DEFAULT_PRAYER_NAME

        val pendingResult = goAsync()
        scope.launch {
            try {
                val reminders = settingsRepository.getPrayerReminders().first()
                val reminder = reminders[prayerName]

                when (reminder?.mode) {
                    ReminderMode.NOTIFICATION -> showNotification(context, prayerName)
                    ReminderMode.ADHAN -> startAdhanService(context, prayerName, reminder.adhanFileName)
                    else -> { /* Do nothing for SILENT */ }
                }

                val prayerTimes = getPrayerTimesUseCase()
                adhanScheduler.scheduleNextWithSettings(prayerTimes, reminders)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle adhan alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startAdhanService(context: Context, prayerName: String, adhanFile: String?) {
        val intent = Intent(context, AdhanService::class.java).apply {
            putExtra(AdhanService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(AdhanService.EXTRA_ADHAN_FILE, adhanFile)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        val soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.muslimvn_notification)
        
        val notification = NotificationCompat.Builder(context, NotificationModule.ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon for now
            .setContentTitle(context.getString(R.string.adhan_notification_title, prayerName))
            .setContentText(context.getString(R.string.adhan_notification_content, prayerName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }

    companion object {
        private const val TAG = "AdhanReceiver"
        private const val DEFAULT_PRAYER_NAME = "Prayer"
    }
}
