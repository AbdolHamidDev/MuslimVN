package com.example.muslimvn.core.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.muslimvn.domain.models.PrayerTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lập lịch báo thức Adhan.
 *
 * Chiến lược "single alarm": tại mọi thời điểm chỉ tồn tại DUY NHẤT một báo thức,
 * dành cho mốc cầu nguyện SẮP TỚI gần nhất. Khi báo thức đó nổ, [AdhanReceiver]
 * sẽ tự tính và lập lịch cho mốc kế tiếp (chaining), thay vì đặt 5 báo thức cùng lúc.
 */
@Singleton
class AdhanScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Mốc cầu nguyện dùng để lập lịch. */
    private data class PrayerAlarm(
        val name: String,
        val timeInMillis: Long
    )

    /**
     * Tìm và chỉ lập MỘT báo thức cho mốc cầu nguyện sắp tới gần nhất.
     *
     * Với mỗi mốc: nếu thời gian đã qua (<= bây giờ) thì KHÔNG đặt cho hôm nay,
     * mà tự động cộng 1 ngày (+24h) để tính cho NGÀY MAI. Sau đó chọn mốc có
     * thời gian nhỏ nhất (luôn ở tương lai).
     */
    fun scheduleNextPrayer(prayerTimes: PrayerTimes) {
        val now = System.currentTimeMillis()

        val candidates = listOf(
            PrayerAlarm(FAJR, prayerTimes.fajr.time),
            PrayerAlarm(DHUHR, prayerTimes.dhuhr.time),
            PrayerAlarm(ASR, prayerTimes.asr.time),
            PrayerAlarm(MAGHRIB, prayerTimes.maghrib.time),
            PrayerAlarm(ISHA, prayerTimes.isha.time)
        ).map { prayer ->
            if (prayer.timeInMillis <= now) {
                // Đã qua giờ hôm nay -> dời sang ngày mai (+24h)
                prayer.copy(timeInMillis = prayer.timeInMillis + ONE_DAY_MILLIS)
            } else {
                prayer
            }
        }

        val next = candidates.minByOrNull { it.timeInMillis } ?: return
        scheduleAdhan(next.name, Date(next.timeInMillis))
    }

    /**
     * Lập báo thức cho một mốc cầu nguyện.
     *
     * [AlarmManager.setExactAndAllowWhileIdle] CHỈ được gọi với mốc thời gian
     * STỨC TƯƠNG LAI (> System.currentTimeMillis()): nếu mốc đã qua thì tự động
     * cộng thêm 1 ngày (+24h); nếu sau khi cộng vẫn không ở tương lai thì bỏ qua.
     */
    fun scheduleAdhan(prayerName: String, prayerTime: Date) {
        val now = System.currentTimeMillis()
        var triggerAtMillis = prayerTime.time

        // Mốc đã qua hôm nay -> không đặt cho hôm nay, dời sang ngày mai (+24h)
        if (triggerAtMillis <= now) {
            triggerAtMillis += ONE_DAY_MILLIS
        }

        // An toàn cuối cùng: tuyệt đối không đặt báo thức trong quá khứ
        if (triggerAtMillis <= now) return

        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = ACTION_ADHAN_ALARM
            putExtra(EXTRA_PRAYER_NAME, prayerName)
        }

        // Request code cố định + FLAG_UPDATE_CURRENT => lần lập lịch mới luôn
        // THAY THẾ báo thức cũ, không bao giờ tích tụ nhiều báo thức.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADHAN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /** Hủy báo thức Adhan đang chờ (nếu có). */
    fun cancelAdhan() {
        val intent = Intent(context, AdhanReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADHAN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val ACTION_ADHAN_ALARM = "ACTION_ADHAN_ALARM"
        const val EXTRA_PRAYER_NAME = "PRAYER_NAME"

        private const val REQUEST_CODE_ADHAN = 2001
        private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

        private const val FAJR = "Fajr"
        private const val DHUHR = "Dhuhr"
        private const val ASR = "Asr"
        private const val MAGHRIB = "Maghrib"
        private const val ISHA = "Isha"
    }
}
