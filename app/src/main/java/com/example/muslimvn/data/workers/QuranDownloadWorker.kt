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
import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.entities.DownloadedAyahEntity
import com.example.muslimvn.domain.repository.QuranRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class QuranDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val quranRepository: QuranRepository,
    private val quranDao: QuranDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val TOTAL_AYAS = 6236
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "quran_download_channel"
    }

    override suspend fun doWork(): Result {
        val reciterId = inputData.getInt("reciterId", -1)
        val surahNumber = inputData.getInt("surahNumber", -1) // -1 means all surahs
        val reciterName = inputData.getString("reciterName") ?: "Reciter"
        
        if (reciterId == -1) return Result.failure()

        // Set as foreground to show notification
        try {
            setForeground(getForegroundInfo(reciterName, 0))
        } catch (e: Exception) {
            // Ignore if foreground fails, though it shouldn't
        }

        return if (surahNumber == -1) {
            downloadFullQuran(reciterId, reciterName)
        } else {
            downloadSingleSurah(reciterId, surahNumber, reciterName)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val reciterName = inputData.getString("reciterName") ?: "Reciter"
        return getForegroundInfo(reciterName, 0)
    }

    private fun getForegroundInfo(reciterName: String, progress: Int): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.quran_download_notification_title))
            .setContentText(context.getString(R.string.quran_download_notification_content, reciterName))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.quran_download_notification_channel)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun downloadFullQuran(reciterId: Int, reciterName: String): Result {
        var downloadedCount = quranDao.getTotalDownloadedAyahsCountSync(reciterId)
        
        for (s in 1..114) {
            if (isStopped) return Result.retry()
            
            val surah = quranRepository.getSurahByNumber(s) ?: continue
            
            // 1. Đảm bảo toàn bộ timing của Surah này được cache vào DB trước
            // fetchAndCacheSurahTiming đã được thiết kế để lưu vào DB trong QuranRepositoryImpl
            val timings = quranRepository.fetchAndCacheSurahTiming(s, reciterId)
            if (timings.isEmpty()) {
                // Nếu không tải được timing (ví dụ lỗi mạng), thử kiểm tra xem trong DB đã có chưa
                // Nếu chưa có cả timing thì không thể biết Audio URL để tải, nên đành bỏ qua Surah này để thử lại sau
                val cachedCount = quranDao.getVerseTimingCountForSurah(s, reciterId)
                if (cachedCount == 0) continue 
            }

            val downloadDir = File(context.filesDir, "audio/$reciterId/$s").apply { mkdirs() }

            for (i in 1..surah.totalAyahs) {
                if (isStopped) return Result.retry()
                
                val verseKey = "$s:$i"
                val isAlreadyDownloaded = quranDao.getDownloadedAyah(verseKey, reciterId) != null
                
                // getVerseTiming sẽ ưu tiên lấy từ DB (vừa được fetch ở trên hoặc đã có sẵn)
                val timing = quranRepository.getVerseTiming(verseKey, reciterId) ?: continue
                val audioUrl = timing.audioUrl ?: continue

                val fileName = "$i.mp3"
                val destinationFile = File(downloadDir, fileName)

                if (!destinationFile.exists()) {
                    val success = downloadFile(audioUrl, destinationFile)
                    if (!success) continue
                }

                if (!isAlreadyDownloaded) {
                    quranDao.insertDownloadedAyah(
                        DownloadedAyahEntity(
                            verseKey = verseKey,
                            reciterId = reciterId,
                            localPath = destinationFile.absolutePath
                        )
                    )
                    downloadedCount++
                }
                
                val progress = (downloadedCount.toFloat() / TOTAL_AYAS)
                setProgress(workDataOf("progress" to progress))
                
                if (downloadedCount % 10 == 0) {
                    try {
                        setForeground(getForegroundInfo(reciterName, (progress * 100).toInt()))
                    } catch (e: Exception) {}
                }
            }
        }
        return Result.success()
    }

    private suspend fun downloadSingleSurah(reciterId: Int, surahNumber: Int, reciterName: String): Result {
        val surah = quranRepository.getSurahByNumber(surahNumber) ?: return Result.failure()
        val totalAyahs = surah.totalAyahs
        val downloadDir = File(context.filesDir, "audio/$reciterId/$surahNumber").apply { mkdirs() }
        
        var downloadedCountInSurah = quranDao.getDownloadedAyahsCount(surahNumber, reciterId)

        for (i in 1..totalAyahs) {
            if (isStopped) return Result.retry()
            
            val verseKey = "$surahNumber:$i"
            val isAlreadyDownloaded = quranDao.getDownloadedAyah(verseKey, reciterId) != null
            
            val timing = quranRepository.getVerseTiming(verseKey, reciterId) ?: continue
            val audioUrl = timing.audioUrl ?: continue

            val fileName = "$i.mp3"
            val destinationFile = File(downloadDir, fileName)

            if (!destinationFile.exists()) {
                val success = downloadFile(audioUrl, destinationFile)
                if (!success) continue
            }

            if (!isAlreadyDownloaded) {
                quranDao.insertDownloadedAyah(
                    DownloadedAyahEntity(
                        verseKey = verseKey,
                        reciterId = reciterId,
                        localPath = destinationFile.absolutePath
                    )
                )
                downloadedCountInSurah++
            }

            val progress = (downloadedCountInSurah.toFloat() / totalAyahs)
            setProgress(workDataOf("progress" to progress))
            
            try {
                setForeground(getForegroundInfo(reciterName, (progress * 100).toInt()))
            } catch (e: Exception) {}
        }
        return Result.success()
    }

    private fun downloadFile(url: String, destination: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
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
}
