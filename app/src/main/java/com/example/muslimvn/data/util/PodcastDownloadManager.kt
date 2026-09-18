package com.example.muslimvn.data.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import com.example.muslimvn.domain.models.PodcastEpisode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PodcastDownloadState {
    data object Idle : PodcastDownloadState
    data object Queued : PodcastDownloadState
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : PodcastDownloadState
    data object Downloaded : PodcastDownloadState
    data class Failed(val error: String) : PodcastDownloadState
}

data class PlaylistDownloadProgress(
    val scholarId: String,
    val totalEpisodes: Int,
    val completedEpisodes: Int,
    val isDownloading: Boolean
)

/**
 * Quản lý tải xuống tập podcast (Single + Playlist) dùng chung:
 * - Giới hạn tối đa 2 download đồng thời.
 * - Ưu tiên tải tập người dùng chủ động yêu cầu trước.
 * - Lưu vị trí file offline vào app-specific storage (`podcasts` directory).
 * - Lưu trạng thái persistent vào Room DB (`PodcastEpisodeDao`).
 * - Tự động phục hồi hoặc hủy các tác vụ bị gián đoạn do ứng dụng bị đóng.
 */
@Singleton
class PodcastDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeDao: PodcastEpisodeDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PodcastDownloadManager"
        const val MAX_PLAYLIST_BATCH_SIZE = 20
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        private const val BUFFER_SIZE = 32 * 1024 // 32KB
        private const val PROGRESS_EMIT_INTERVAL_MS = 250L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Danh sách ưu tiên các episodeId đang chờ tải trong Queue. */
    private val downloadQueue = mutableListOf<String>()

    /** Lưu trữ các Coroutine Job đang thực thi tải xuống theo episodeId. */
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /** Metadata tạm thời của episode được enqueue phục vụ tải xuống. */
    private val episodeCache = ConcurrentHashMap<String, PodcastEpisode>()

    private val _downloadStates = MutableStateFlow<Map<String, PodcastDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, PodcastDownloadState>> = _downloadStates.asStateFlow()

    private val _playlistProgresses = MutableStateFlow<Map<String, PlaylistDownloadProgress>>(emptyMap())
    val playlistProgresses: StateFlow<Map<String, PlaylistDownloadProgress>> = _playlistProgresses.asStateFlow()

    init {
        // Kiểm tra toàn vẹn file offline và khôi phục queue khi khởi động ứng dụng
        scope.launch {
            verifyIntegrityAndRestoreQueue()
        }
    }

    /** Trả về đường dẫn thư mục lưu trữ file podcast offline của ứng dụng. */
    fun getDownloadDir(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: context.filesDir, "podcasts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** Trả về file offline của một tập podcast theo id. */
    fun getDownloadedFile(episodeId: String): File {
        val safeFileName = "podcast_${episodeId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.mp3"
        return File(getDownloadDir(), safeFileName)
    }

    /** Trả về trạng thái tải hiện tại của một episodeId. */
    fun getDownloadState(episodeId: String): PodcastDownloadState {
        return _downloadStates.value[episodeId] ?: PodcastDownloadState.Idle
    }

    /**
     * Yêu cầu tải một tập đơn lẻ:
     * - Nếu tập đã tải xong & file hợp lệ: cập nhật trạng thái Downloaded.
     * - Nếu tập đang nằm trong Queue: đẩy tập lên đầu Queue (ưu tiên cao hơn).
     * - Nếu tập chưa có trong Queue: thêm vào đầu Queue và bắt đầu xử lý.
     */
    fun enqueueEpisode(episode: PodcastEpisode, isPriority: Boolean = true) {
        scope.launch {
            val targetFile = getDownloadedFile(episode.id)
            if (targetFile.exists() && targetFile.length() > 0) {
                updateEpisodeState(episode.id, PodcastDownloadState.Downloaded)
                episodeDao.updateDownloadStatus(episode.id, true, targetFile.absolutePath, "DOWNLOADED")
                return@launch
            }

            episodeCache[episode.id] = episode

            mutex.withLock {
                val currentState = _downloadStates.value[episode.id]
                if (currentState is PodcastDownloadState.Downloading) {
                    // Đang trong quá trình tải -> không tạo trùng lặp
                    return@withLock
                }

                if (downloadQueue.contains(episode.id)) {
                    if (isPriority) {
                        // Di chuyển tập lên đầu danh sách chờ
                        downloadQueue.remove(episode.id)
                        downloadQueue.add(0, episode.id)
                    }
                } else {
                    if (isPriority) {
                        downloadQueue.add(0, episode.id)
                    } else {
                        downloadQueue.add(episode.id)
                    }
                    updateEpisodeState(episode.id, PodcastDownloadState.Queued)
                    episodeDao.updateDownloadStatus(episode.id, false, null, "QUEUED")
                }
            }

            processNextDownloads()
        }
    }

    /** Trả về danh sách các tập chưa tải (tối đa [maxBatchSize] tập mới nhất) để xem trước thông tin batch. */
    fun getPendingBatch(episodes: List<PodcastEpisode>, maxBatchSize: Int = MAX_PLAYLIST_BATCH_SIZE): List<PodcastEpisode> {
        val pending = mutableListOf<PodcastEpisode>()
        for (ep in episodes) {
            val targetFile = getDownloadedFile(ep.id)
            val currentState = _downloadStates.value[ep.id]
            val isAlreadyDownloaded = ep.isDownloaded && targetFile.exists() && targetFile.length() > 0

            if (!isAlreadyDownloaded && currentState !is PodcastDownloadState.Downloading && currentState !is PodcastDownloadState.Queued) {
                pending.add(ep)
            }
        }
        return pending.take(maxBatchSize)
    }

    /**
     * Tải danh sách tập của một học giả (Playlist) theo lô (Batch):
     * - Giới hạn tối đa [maxBatchSize] tập chưa tải (mặc định 20 tập mới nhất).
     * - Lọc bỏ các tập đã tải xong hoặc đã nằm trong Queue/Downloading.
     * - Thêm các tập chưa tải vào Queue.
     * - Cập nhật tiến trình tổng dạng x/y tập cho UI.
     */
    fun enqueuePlaylist(scholarId: String, episodes: List<PodcastEpisode>, maxBatchSize: Int = MAX_PLAYLIST_BATCH_SIZE) {
        scope.launch {
            val batch = getPendingBatch(episodes, maxBatchSize)

            if (batch.isEmpty()) {
                val completedCount = episodes.count { ep ->
                    val f = getDownloadedFile(ep.id)
                    ep.isDownloaded && f.exists() && f.length() > 0
                }
                _playlistProgresses.update { map ->
                    map + (scholarId to PlaylistDownloadProgress(scholarId, totalEpisodes = episodes.size, completedEpisodes = completedCount, isDownloading = false))
                }
                return@launch
            }

            // Cập nhật tiến trình ban đầu cho lô tải hiện tại
            _playlistProgresses.update { map ->
                map + (scholarId to PlaylistDownloadProgress(scholarId, totalEpisodes = batch.size, completedEpisodes = 0, isDownloading = true))
            }

            for (ep in batch) {
                enqueueEpisode(ep, isPriority = false)
            }
        }
    }

    /** Hủy toàn bộ tiến trình tải xuống playlist của một học giả. */
    fun cancelPlaylistDownload(scholarId: String) {
        scope.launch {
            val targetIds = mutableListOf<String>()

            mutex.withLock {
                val queueIterator = downloadQueue.iterator()
                while (queueIterator.hasNext()) {
                    val id = queueIterator.next()
                    val ep = episodeCache[id]
                    if (ep?.scholarId == scholarId) {
                        queueIterator.remove()
                        targetIds.add(id)
                    }
                }

                activeJobs.keys.forEach { id ->
                    val ep = episodeCache[id]
                    if (ep?.scholarId == scholarId) {
                        activeJobs[id]?.cancel()
                        targetIds.add(id)
                    }
                }
            }

            for (id in targetIds.distinct()) {
                val tempFile = File(getDownloadDir(), "podcast_${id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.tmp")
                if (tempFile.exists()) tempFile.delete()

                updateEpisodeState(id, PodcastDownloadState.Idle)
                episodeDao.updateDownloadStatus(id, false, null, "IDLE")
            }

            _playlistProgresses.update { map ->
                val current = map[scholarId]
                if (current != null) {
                    map + (scholarId to current.copy(isDownloading = false))
                } else {
                    map
                }
            }

            processNextDownloads()
        }
    }

    /** Hủy tác vụ tải một tập podcast. */
    fun cancelDownload(episodeId: String) {
        scope.launch {
            mutex.withLock {
                downloadQueue.remove(episodeId)
                activeJobs.remove(episodeId)?.cancel()
            }
            val tempFile = File(getDownloadDir(), "podcast_${episodeId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.tmp")
            if (tempFile.exists()) tempFile.delete()

            updateEpisodeState(episodeId, PodcastDownloadState.Idle)
            episodeDao.updateDownloadStatus(episodeId, false, null, "IDLE")

            processNextDownloads()
        }
    }

    /** Xóa bản tải xuống offline của một tập podcast. */
    fun deleteDownloadedFile(episode: PodcastEpisode) {
        scope.launch {
            cancelDownload(episode.id)
            val file = getDownloadedFile(episode.id)
            if (file.exists()) {
                file.delete()
            }
            updateEpisodeState(episode.id, PodcastDownloadState.Idle)
            episodeDao.updateDownloadStatus(episode.id, false, null, "IDLE")
        }
    }

    /** Xử lý vòng lặp lấy các tập trong Queue ra tải xuống dựa trên hạn mức đồng thời (MAX_CONCURRENT_DOWNLOADS). */
    private suspend fun processNextDownloads() {
        mutex.withLock {
            while (activeJobs.size < MAX_CONCURRENT_DOWNLOADS && downloadQueue.isNotEmpty()) {
                val nextEpisodeId = downloadQueue.removeAt(0)
                val episode = episodeCache[nextEpisodeId] ?: run {
                    episodeDao.getEpisodeById(nextEpisodeId)?.let { entity ->
                        PodcastEpisode(
                            id = entity.id,
                            scholarId = entity.scholarId,
                            title = entity.title,
                            audioUrl = entity.audioUrl,
                            artworkUrl = entity.artworkUrl,
                            duration = entity.duration,
                            pubDate = entity.pubDate,
                            description = entity.description,
                            isDownloaded = entity.isDownloaded,
                            lastPositionMs = entity.lastPositionMs,
                            localFilePath = entity.localFilePath,
                            downloadStatus = entity.downloadStatus
                        )
                    }
                }

                if (episode == null) continue

                val job = scope.launch {
                    downloadEpisodeInternal(episode)
                }
                activeJobs[nextEpisodeId] = job
            }
        }
    }

    /** Thực thi tải file audio từ mạng bằng OkHttpClient và lưu vào file tạm (.tmp). */
    private suspend fun downloadEpisodeInternal(episode: PodcastEpisode) {
        val targetFile = getDownloadedFile(episode.id)
        val tempFile = File(getDownloadDir(), "podcast_${episode.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.tmp")

        if (targetFile.exists() && targetFile.length() > 0) {
            updateEpisodeState(episode.id, PodcastDownloadState.Downloaded)
            episodeDao.updateDownloadStatus(episode.id, true, targetFile.absolutePath, "DOWNLOADED")
            onDownloadTaskFinished(episode.id, episode.scholarId, success = true)
            return
        }

        updateEpisodeState(episode.id, PodcastDownloadState.Downloading(0.0f, 0L, 0L))
        episodeDao.updateDownloadStatus(episode.id, false, null, "DOWNLOADING")

        try {
            val request = Request.Builder()
                .url(episode.audioUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw java.io.IOException("HTTP error code: ${response.code}")
            }

            val body = response.body ?: throw java.io.IOException("Empty response body")
            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var lastEmitTime = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= PROGRESS_EMIT_INTERVAL_MS) {
                            lastEmitTime = now
                            val progress = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                            updateEpisodeState(episode.id, PodcastDownloadState.Downloading(progress, bytesDownloaded, totalBytes))
                        }
                    }
                    outputStream.flush()
                }
            }

            // Hoàn tất tải xuống -> Đổi tên file tạm thành file chính thức .mp3
            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                updateEpisodeState(episode.id, PodcastDownloadState.Downloaded)
                episodeDao.updateDownloadStatus(episode.id, true, targetFile.absolutePath, "DOWNLOADED")
                onDownloadTaskFinished(episode.id, episode.scholarId, success = true)
            } else {
                throw java.io.IOException("File downloaded is empty")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tải tập podcast (${episode.title}): ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()

            val errorMessage = e.message ?: "Tải xuống thất bại"
            updateEpisodeState(episode.id, PodcastDownloadState.Failed(errorMessage))
            episodeDao.updateDownloadStatus(episode.id, false, null, "FAILED")
            onDownloadTaskFinished(episode.id, episode.scholarId, success = false)
        }
    }

    /** Gọi sau khi 1 tác vụ kết thúc để dọn dẹp và kích hoạt tác vụ tiếp theo trong Queue. */
    private suspend fun onDownloadTaskFinished(episodeId: String, scholarId: String, success: Boolean) {
        mutex.withLock {
            activeJobs.remove(episodeId)
        }

        // Cập nhật tiến trình tổng của Playlist nếu có
        _playlistProgresses.value[scholarId]?.let { current ->
            val newCompleted = if (success) current.completedEpisodes + 1 else current.completedEpisodes
            val remainingInScholar = downloadQueue.count { id ->
                episodeCache[id]?.scholarId == scholarId
            } + activeJobs.keys.count { id ->
                episodeCache[id]?.scholarId == scholarId
            }
            val isStillDownloading = remainingInScholar > 0
            _playlistProgresses.update { map ->
                map + (scholarId to current.copy(completedEpisodes = newCompleted, isDownloading = isStillDownloading))
            }
        }

        processNextDownloads()
    }

    private fun updateEpisodeState(episodeId: String, state: PodcastDownloadState) {
        _downloadStates.update { map ->
            map + (episodeId to state)
        }
    }

    /** Kiểm tra tính toàn vẹn cơ sở dữ liệu và khôi phục các tác vụ dở dang khi khởi động app. */
    private suspend fun verifyIntegrityAndRestoreQueue() {
        runCatching {
            // 1. Kiểm tra các tập đánh dấu DOWNLOADED nhưng file thực tế bị mất
            val downloadedEntities = episodeDao.getDownloadedEpisodesOnce()
            for (entity in downloadedEntities) {
                val file = getDownloadedFile(entity.id)
                if (!file.exists() || file.length() == 0L) {
                    Log.w(TAG, "File podcast không tồn tại (${entity.title}), khôi phục trạng thái IDLE")
                    episodeDao.updateDownloadStatus(entity.id, false, null, "IDLE")
                    updateEpisodeState(entity.id, PodcastDownloadState.Idle)
                } else {
                    updateEpisodeState(entity.id, PodcastDownloadState.Downloaded)
                }
            }

            // 2. Khôi phục các tập đang QUEUED hoặc DOWNLOADING lúc app bị force close
            val nonIdleEntities = episodeDao.getNonIdleEpisodesOnce()
            for (entity in nonIdleEntities) {
                if (entity.downloadStatus == "QUEUED" || entity.downloadStatus == "DOWNLOADING") {
                    val episode = PodcastEpisode(
                        id = entity.id,
                        scholarId = entity.scholarId,
                        title = entity.title,
                        audioUrl = entity.audioUrl,
                        artworkUrl = entity.artworkUrl,
                        duration = entity.duration,
                        pubDate = entity.pubDate,
                        description = entity.description,
                        isDownloaded = false,
                        lastPositionMs = entity.lastPositionMs,
                        localFilePath = entity.localFilePath,
                        downloadStatus = entity.downloadStatus
                    )
                    enqueueEpisode(episode, isPriority = false)
                }
            }
        }.onFailure {
            Log.e(TAG, "Lỗi kiểm tra toàn vẹn file podcast khi khởi động", it)
        }
    }
}
