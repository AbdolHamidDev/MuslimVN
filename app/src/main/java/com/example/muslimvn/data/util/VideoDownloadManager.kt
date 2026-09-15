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
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadMediaType {
    VIDEO, AUDIO
}

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
    private val youtubeRepository: YoutubeRepository,
    private val okHttpClient: OkHttpClient
) {
    private companion object {
        /** A few independent requests avoid the per-connection throttling seen on YouTube DASH audio. */
        const val AUDIO_PART_SIZE_BYTES = 4L * 1024L * 1024L
        const val AUDIO_PARALLEL_REQUESTS = 4
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    
    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = _downloadStatuses.asStateFlow()

    fun getDownloadStatusKey(videoId: String, mediaType: DownloadMediaType): String {
        return "${videoId}_${mediaType.name}"
    }

    fun getStatusFlow(videoId: String, mediaType: DownloadMediaType = DownloadMediaType.VIDEO): Flow<DownloadStatus> {
        val key = getDownloadStatusKey(videoId, mediaType)
        return kotlinx.coroutines.flow.combine(
            downloadStatuses,
            downloadedVideoDao.getAllDownloadedVideos()
        ) { statuses, downloadedList ->
            statuses[key] ?: if (downloadedList.any { it.id == key || (it.videoId == videoId && it.mediaType == mediaType.name) || (mediaType == DownloadMediaType.VIDEO && it.id == videoId) }) {
                DownloadStatus.Completed
            } else {
                DownloadStatus.Idle
            }
        }
    }

    fun getVideoAndAudioStatuses(videoId: String): Flow<Pair<DownloadStatus, DownloadStatus>> {
        val videoKey = getDownloadStatusKey(videoId, DownloadMediaType.VIDEO)
        val audioKey = getDownloadStatusKey(videoId, DownloadMediaType.AUDIO)
        
        return kotlinx.coroutines.flow.combine(
            downloadStatuses,
            downloadedVideoDao.getDownloadedMediaByVideoId(videoId)
        ) { statuses, downloadedList ->
            val videoStatus = statuses[videoKey]
                ?: if (downloadedList.any { it.id == videoKey || (it.videoId == videoId && it.mediaType == DownloadMediaType.VIDEO.name) || (it.id == videoId && (it.mediaType == "VIDEO" || it.mediaType.isEmpty())) }) {
                    DownloadStatus.Completed
                } else {
                    DownloadStatus.Idle
                }
                
            val audioStatus = statuses[audioKey]
                ?: if (downloadedList.any { it.id == audioKey || (it.videoId == videoId && it.mediaType == DownloadMediaType.AUDIO.name) }) {
                    DownloadStatus.Completed
                } else {
                    DownloadStatus.Idle
                }
                
            Pair(videoStatus, audioStatus)
        }
    }

    val allDownloadedVideos: Flow<List<DownloadedVideoEntity>> = downloadedVideoDao.getAllDownloadedVideos()

    fun getDownloadedMediaByType(mediaType: DownloadMediaType): Flow<List<DownloadedVideoEntity>> {
        return downloadedVideoDao.getDownloadedMediaByType(mediaType.name)
    }

    fun startDownload(
        videoId: String,
        videoUrl: String,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        duration: String,
        mediaType: DownloadMediaType = DownloadMediaType.VIDEO
    ) {
        val downloadKey = getDownloadStatusKey(videoId, mediaType)
        if (activeJobs.containsKey(downloadKey) && activeJobs[downloadKey]?.isActive == true) return

        val job = scope.launch {
            updateStatus(downloadKey, DownloadStatus.Downloading(0f, 0L, 1L))
            try {
                // 1. Lấy stream URL từ YouTube theo mediaType
                var streamUrl: String? = null
                val streamFlow = if (mediaType == DownloadMediaType.AUDIO) {
                    youtubeRepository.getAudioStreamUrl(videoUrl)
                } else {
                    youtubeRepository.getVideoStreamUrl(videoUrl)
                }

                streamFlow.collect { result ->
                    result.onSuccess { url ->
                        streamUrl = url
                    }.onFailure { e ->
                        throw Exception(e.message ?: "Không thể lấy luồng ${if (mediaType == DownloadMediaType.AUDIO) "âm thanh" else "video"}")
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
                // YouTube trả về stream M4A; lưu đúng định dạng thay vì đổi tên thành MP3.
                val ext = if (mediaType == DownloadMediaType.AUDIO) "m4a" else "mp4"
                val targetFile = File(downloadsDir, "MuslimVN_$sanitizedTitle-$downloadKey.$ext")

                var downloadedBytes = if (targetFile.exists()) targetFile.length() else 0L
                var totalBytes = -1L

                val requestBuilder = okhttp3.Request.Builder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")

                // An M4A from YouTube is normally a DASH stream. Ask for a range even from byte
                // zero so the server gives us the complete size in Content-Range.
                if (downloadedBytes > 0 || mediaType == DownloadMediaType.AUDIO) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                }

                val downloadClient = okHttpClient.newBuilder()
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .build()

                var currentUrl = finalStreamUrl
                var response = downloadClient.newCall(requestBuilder.url(currentUrl).build()).execute()
                var responseCode = response.code

                if (responseCode != 200 && responseCode != 206 && mediaType == DownloadMediaType.AUDIO) {
                    response.close()
                    var fallbackUrl: String? = null
                    youtubeRepository.getVideoStreamUrl(videoUrl).collect { res ->
                        res.onSuccess { fallbackUrl = it }
                    }
                    if (!fallbackUrl.isNullOrEmpty() && fallbackUrl != currentUrl) {
                        currentUrl = fallbackUrl!!
                        val fallbackReq = okhttp3.Request.Builder()
                            .url(currentUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .header("Accept", "*/*")
                            .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                            .apply {
                                if (downloadedBytes > 0) header("Range", "bytes=$downloadedBytes-")
                            }
                            .build()
                        response = downloadClient.newCall(fallbackReq).execute()
                        responseCode = response.code
                    }
                }

                if (responseCode == 200 || responseCode == 206) {
                    val body = response.body ?: throw Exception("Nội dung phản hồi rỗng")
                    val contentLength = body.contentLength()
                    totalBytes = if (responseCode == 206) {
                        response.header("Content-Range")
                            ?.substringAfterLast('/')
                            ?.toLongOrNull()
                            ?: contentLength + downloadedBytes
                    } else {
                        contentLength
                    }

                    // A fresh audio download can safely be split into resumable temporary parts.
                    // Do not split an old partially-downloaded target file: it was created by the
                    // former sequential downloader and is only known to contain a contiguous prefix.
                    if (
                        mediaType == DownloadMediaType.AUDIO &&
                        downloadedBytes == 0L &&
                        responseCode == 206 &&
                        totalBytes > AUDIO_PART_SIZE_BYTES
                    ) {
                        response.close()
                        downloadAudioInParallel(
                            downloadKey = downloadKey,
                            streamUrl = currentUrl,
                            targetFile = targetFile,
                            totalBytes = totalBytes,
                            downloadClient = downloadClient
                        )
                        downloadedBytes = targetFile.length()
                    } else {
                        val inputStream = body.byteStream()
                        val randomAccessFile = RandomAccessFile(targetFile, "rw")
                        if (responseCode == 206) {
                            randomAccessFile.seek(downloadedBytes)
                        } else {
                            randomAccessFile.setLength(0)
                            downloadedBytes = 0
                        }

                        val buffer = ByteArray(32 * 1024)
                        var bytesRead: Int
                        var lastProgressUpdateMs = 0L

                        try {
                            while (isActive) {
                                bytesRead = inputStream.read(buffer)
                                if (bytesRead == -1) break

                                randomAccessFile.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdateMs >= PROGRESS_UPDATE_INTERVAL_MS) {
                                    lastProgressUpdateMs = now
                                    publishProgress(downloadKey, downloadedBytes, totalBytes)
                                }
                            }
                        } finally {
                            inputStream.close()
                            randomAccessFile.close()
                            response.close()
                        }
                    }

                    if (isActive && (totalBytes <= 0 || downloadedBytes >= totalBytes)) {
                        updateStatus(downloadKey, DownloadStatus.Completed)
                        val entity = DownloadedVideoEntity(
                            id = downloadKey,
                            videoId = videoId,
                            mediaType = mediaType.name,
                            title = title,
                            thumbnailUrl = thumbnailUrl,
                            uploaderName = uploaderName,
                            localFilePath = targetFile.absolutePath,
                            duration = duration,
                            downloadDate = System.currentTimeMillis(),
                            fileSize = targetFile.length()
                        )
                        downloadedVideoDao.insertDownloadedVideo(entity)
                        activeJobs.remove(downloadKey)
                    } else {
                        updateStatus(downloadKey, DownloadStatus.Paused)
                    }
                } else {
                    response.close()
                    throw Exception("Lỗi HTTP: $responseCode")
                }
            } catch (e: CancellationException) {
                updateStatus(downloadKey, DownloadStatus.Paused)
            } catch (e: Exception) {
                updateStatus(downloadKey, DownloadStatus.Failed(e.message ?: "Lỗi tải xuống"))
                activeJobs.remove(downloadKey)
            }
        }

        activeJobs[downloadKey] = job
    }

    fun pauseDownload(videoId: String, mediaType: DownloadMediaType = DownloadMediaType.VIDEO) {
        val downloadKey = getDownloadStatusKey(videoId, mediaType)
        activeJobs[downloadKey]?.cancel()
        activeJobs.remove(downloadKey)
        updateStatus(downloadKey, DownloadStatus.Paused)
    }

    fun resumeDownload(
        videoId: String,
        videoUrl: String,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        duration: String,
        mediaType: DownloadMediaType = DownloadMediaType.VIDEO
    ) {
        startDownload(videoId, videoUrl, title, thumbnailUrl, uploaderName, duration, mediaType)
    }

    suspend fun deleteDownloadedVideo(idOrVideoId: String) {
        val video = downloadedVideoDao.getDownloadedVideoById(idOrVideoId)
        if (video != null) {
            val file = File(video.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            downloadedVideoDao.deleteDownloadedVideo(video.id)
            updateStatus(video.id, DownloadStatus.Idle)
        } else {
            // Delete video format
            val videoEntity = downloadedVideoDao.getDownloadedVideoById("${idOrVideoId}_VIDEO")
            if (videoEntity != null) {
                File(videoEntity.localFilePath).takeIf { it.exists() }?.delete()
                downloadedVideoDao.deleteDownloadedVideo(videoEntity.id)
                updateStatus("${idOrVideoId}_VIDEO", DownloadStatus.Idle)
            }
            // Delete audio format
            val audioEntity = downloadedVideoDao.getDownloadedVideoById("${idOrVideoId}_AUDIO")
            if (audioEntity != null) {
                File(audioEntity.localFilePath).takeIf { it.exists() }?.delete()
                downloadedVideoDao.deleteDownloadedVideo(audioEntity.id)
                updateStatus("${idOrVideoId}_AUDIO", DownloadStatus.Idle)
            }
        }
    }

    private fun updateStatus(key: String, status: DownloadStatus) {
        _downloadStatuses.update { current -> current + (key to status) }
    }

    private fun publishProgress(downloadKey: String, downloadedBytes: Long, totalBytes: Long) {
        val progress = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        updateStatus(
            downloadKey,
            DownloadStatus.Downloading(progress, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes)
        )
    }

    /**
     * Downloads a DASH audio stream in independent HTTP ranges. Each range has its own temporary
     * file, so a later tap can resume each verified part before they are merged in order.
     */
    private suspend fun downloadAudioInParallel(
        downloadKey: String,
        streamUrl: String,
        targetFile: File,
        totalBytes: Long,
        downloadClient: OkHttpClient
    ) = coroutineScope {
        val partsDir = File(targetFile.parentFile, "${targetFile.name}.parts").apply { mkdirs() }
        val ranges = generateSequence(0L) { start ->
            (start + AUDIO_PART_SIZE_BYTES).takeIf { it < totalBytes }
        }.map { start -> start..minOf(start + AUDIO_PART_SIZE_BYTES - 1, totalBytes - 1) }.toList()
        val downloaded = AtomicLong(
            ranges.sumOf { range ->
                val file = partFile(partsDir, range)
                file.length().coerceAtMost(range.last - range.first + 1)
            }
        )
        val progressLock = Any()
        var lastProgressUpdateMs = 0L
        publishProgress(downloadKey, downloaded.get(), totalBytes)

        ranges.chunked(AUDIO_PARALLEL_REQUESTS).forEach { batch ->
            batch.map { range ->
                async {
                    val expectedLength = range.last - range.first + 1
                    val partFile = partFile(partsDir, range)
                    var localBytes = partFile.length()
                    if (localBytes > expectedLength) {
                        partFile.delete()
                        downloaded.addAndGet(-expectedLength)
                        localBytes = 0L
                    }
                    if (localBytes == expectedLength) return@async

                    val requestedStart = range.first + localBytes
                    val request = okhttp3.Request.Builder()
                        .url(streamUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "*/*")
                        .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Range", "bytes=$requestedStart-${range.last}")
                        .build()
                    val response = downloadClient.newCall(request).execute()
                    try {
                        val expectedContentRange = "bytes $requestedStart-${range.last}/"
                        if (response.code != 206 || response.header("Content-Range")?.startsWith(expectedContentRange) != true) {
                            throw Exception("Máy chủ trả về range M4A không hợp lệ (HTTP ${response.code})")
                        }
                        val body = response.body ?: throw Exception("Nội dung phản hồi rỗng")
                        body.byteStream().use { input ->
                            FileOutputStream(partFile, true).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                while (isActive) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    val current = downloaded.addAndGet(read.toLong())
                                    val now = System.currentTimeMillis()
                                    synchronized(progressLock) {
                                        if (now - lastProgressUpdateMs >= PROGRESS_UPDATE_INTERVAL_MS) {
                                            lastProgressUpdateMs = now
                                            publishProgress(downloadKey, current, totalBytes)
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        response.close()
                    }
                    if (partFile.length() != expectedLength) throw CancellationException("Tải M4A đã dừng")
                }
            }.awaitAll()
        }

        if (!isActive) throw CancellationException("Tải M4A đã dừng")
        FileOutputStream(targetFile, false).use { output ->
            ranges.forEach { range ->
                partFile(partsDir, range).inputStream().use { input -> input.copyTo(output) }
            }
        }
        if (targetFile.length() != totalBytes) throw Exception("Kích thước M4A sau khi ghép không khớp")
        partsDir.deleteRecursively()
        publishProgress(downloadKey, totalBytes, totalBytes)
    }

    private fun partFile(partsDir: File, range: LongRange): File =
        File(partsDir, "${range.first}-${range.last}.part")
}
