package com.example.muslimvn.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.muslimvn.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class MushafDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val TOTAL_PAGES = 604
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "mushaf_download_channel"
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo(0))
        } catch (e: Exception) {}

        val downloadDir = File(context.filesDir, "mushaf").apply { mkdirs() }
        var downloadedCount = 0

        // Kiểm tra xem đã có bao nhiêu trang rồi
        for (i in 1..TOTAL_PAGES) {
            val formattedPage = i.toString().padStart(3, '0')
            if (File(downloadDir, "page$formattedPage.png").exists()) {
                downloadedCount++
            }
        }

        for (i in 1..TOTAL_PAGES) {
            if (isStopped) return Result.retry()

            val formattedPage = i.toString().padStart(3, '0')
            val destinationFile = File(downloadDir, "page$formattedPage.png")

            if (!destinationFile.exists()) {
                val imageUrl = "https://android.quran.com/data/width_1024/page$formattedPage.png"
                val success = downloadFile(imageUrl, destinationFile)
                if (success) {
                    downloadedCount++
                }
            } else {
                // Đã có rồi, không cần tải lại
            }

            if (i % 5 == 0 || i == TOTAL_PAGES) {
                val progress = (downloadedCount.toFloat() / TOTAL_PAGES)
                setProgress(workDataOf("progress" to progress))
                try {
                    setForeground(getForegroundInfo((progress * 100).toInt()))
                } catch (e: Exception) {}
            }
        }

        return Result.success()
    }

    private fun downloadFile(url: String, destination: File): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return false

            response.body?.byteStream()?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getForegroundInfo(progress: Int): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Đang tải dữ liệu Mushaf")
            .setContentText("Tiến độ: $progress%")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mushaf Download"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
