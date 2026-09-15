package com.example.muslimvn.data.util

import android.content.Context
import android.os.Environment
import com.example.muslimvn.data.local.dao.DownloadedVideoDao
import com.example.muslimvn.data.local.entities.DownloadedVideoEntity
import com.example.muslimvn.domain.repository.YoutubeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus
    data object Paused : DownloadStatus
    data object Completed : DownloadStatus
    data class Failed(val error: String) : DownloadStatus
}

@Singleton
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedVideoDao: DownloadedVideoDao,
    private val youtubeRepository: YoutubeRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    
    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = _downloadStatuses.asStateFlow()

    fun getStatusFlow(videoId: String): Flow<DownloadStatus> {
        return kotlinx.coroutines.flow.combine(
            downloadStatuses,
            downloadedVideoDao.getAllDownloadedVideos()
        ) { statuses, downloadedList ->
            statuses[videoId] ?: if (downloadedList.any { it.id == videoId }) {
                DownloadStatus.Completed
            } else {
                DownloadStatus.Idle
            }
        }
    }

    val allDownloadedVideos: Flow<List<DownloadedVideoEntity>> = downloadedVideoDao.getAllDownloadedVideos()

    fun startDownload(
        videoId: String,
        videoUrl: String,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        duration: String
    ) {
        if (activeJobs.containsKey(videoId) && activeJobs[videoId]?.isActive == true) return

        val job = scope.launch {
            updateStatus(videoId, DownloadStatus.Downloading(0f, 0L, 1L))
            try {
                // 1. Lấy stream URL từ YouTube
                var streamUrl: String? = null
                youtubeRepository.getVideoStreamUrl(videoUrl).collect { result ->
                    result.onSuccess { url ->
                        streamUrl = url
                    }.onFailure { e ->
                        throw Exception(e.message ?: "Không thể lấy luồng video")
                    }
                }

                val finalStreamUrl = streamUrl ?: throw Exception("Stream URL trống")

                // 2. Chuẩn bị tệp đích trong thư mục Downloads công khai của máy
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                // Làm sạch tên file hợp lệ
                val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(50)
                val targetFile = File(downloadsDir, "MuslimVN_$sanitizedTitle-$videoId.mp4")

                var downloadedBytes = if (targetFile.exists()) targetFile.length() else 0L
                var totalBytes = -1L

                val connection = URL(finalStreamUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                // Kiểm tra hỗ trợ Range nếu file đã tải một phần
                if (downloadedBytes > 0) {
                    connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                    val contentLength = connection.contentLengthLong
                    totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        contentLength + downloadedBytes
                    } else {
                        contentLength
                    }

                    val inputStream = connection.inputStream
                    val randomAccessFile = RandomAccessFile(targetFile, "rw")
                    if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        randomAccessFile.seek(downloadedBytes)
                    } else {
                        randomAccessFile.setLength(0)
                        downloadedBytes = 0
                    }

                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    try {
                        while (isActive) {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break

                            randomAccessFile.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progress = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            updateStatus(videoId, DownloadStatus.Downloading(progress, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes))
                        }
                    } finally {
                        inputStream.close()
                        randomAccessFile.close()
                    }

                    // Nếu tải hoàn tất (không bị cancel giữa chừng)
                    if (isActive && (totalBytes <= 0 || downloadedBytes >= totalBytes)) {
                        updateStatus(videoId, DownloadStatus.Completed)
                        val entity = DownloadedVideoEntity(
                            id = videoId,
                            title = title,
                            thumbnailUrl = thumbnailUrl,
                            uploaderName = uploaderName,
                            localFilePath = targetFile.absolutePath,
                            duration = duration,
                            downloadDate = System.currentTimeMillis(),
                            fileSize = targetFile.length()
                        )
                        downloadedVideoDao.insertDownloadedVideo(entity)
                        activeJobs.remove(videoId)
                    } else {
                        updateStatus(videoId, DownloadStatus.Paused)
                    }
                } else {
                    throw Exception("Lỗi HTTP: $responseCode")
                }
            } catch (e: CancellationException) {
                updateStatus(videoId, DownloadStatus.Paused)
            } catch (e: Exception) {
                updateStatus(videoId, DownloadStatus.Failed(e.message ?: "Lỗi tải xuống"))
                activeJobs.remove(videoId)
            }
        }

        activeJobs[videoId] = job
    }

    fun pauseDownload(videoId: String) {
        activeJobs[videoId]?.cancel()
        activeJobs.remove(videoId)
        updateStatus(videoId, DownloadStatus.Paused)
    }

    fun resumeDownload(
        videoId: String,
        videoUrl: String,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        duration: String
    ) {
        startDownload(videoId, videoUrl, title, thumbnailUrl, uploaderName, duration)
    }

    suspend fun deleteDownloadedVideo(videoId: String) {
        val video = downloadedVideoDao.getDownloadedVideoById(videoId)
        if (video != null) {
            val file = File(video.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            downloadedVideoDao.deleteDownloadedVideo(videoId)
        }
        updateStatus(videoId, DownloadStatus.Idle)
    }

    private fun updateStatus(videoId: String, status: DownloadStatus) {
        val currentMap = _downloadStatuses.value.toMutableMap()
        currentMap[videoId] = status
        _downloadStatuses.value = currentMap
    }
}
