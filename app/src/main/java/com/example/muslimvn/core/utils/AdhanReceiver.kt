package com.example.muslimvn.core.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.muslimvn.R
import com.example.muslimvn.core.di.NotificationModule
import com.example.muslimvn.domain.usecases.GetPrayerTimesUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AdhanScheduler.ACTION_ADHAN_ALARM) return

        val prayerName =
            intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER_NAME) ?: DEFAULT_PRAYER_NAME

        showNotification(context, prayerName)

        // Chaining: ngay khi báo thức vừa nổ, tính lại và lập lịch cho DUY NHẤT
        // mốc cầu nguyện tiếp theo (goAsync để giữ process sống tới khi xong).
        val pendingResult = goAsync()
        scope.launch {
            try {
                val prayerTimes = getPrayerTimesUseCase()
                adhanScheduler.scheduleNextPrayer(prayerTimes)
            } catch (e: Exception) {
                // Không để lỗi mạng/vị trí làm crash receiver; chuỗi lập lịch sẽ
                // được khôi phục ở lần mở app hoặc refresh kế tiếp.
                Log.e(TAG, "Failed to schedule next adhan alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        val notification = NotificationCompat.Builder(context, NotificationModule.ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon for now
            .setContentTitle(context.getString(R.string.adhan_notification_title, prayerName))
            .setContentText(context.getString(R.string.adhan_notification_content, prayerName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }

    companion object {
        private const val TAG = "AdhanReceiver"
        private const val DEFAULT_PRAYER_NAME = "Prayer"
    }
}
