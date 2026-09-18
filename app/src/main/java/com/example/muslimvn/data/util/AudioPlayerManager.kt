package com.example.muslimvn.data.util

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.MediaSession
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AudioPlayItem(
    val url: String,
    val mediaId: String,
    val title: String? = null,
    val artist: String? = null,
    val artworkPath: String? = null
)

/**
 * Trình phát audio dùng chung toàn app (Media3 ExoPlayer), phục vụ cả:
 *  - Quran: [play] giữ nguyên API cũ (url + mediaId + callback kết thúc để nối ayah).
 *  - Podcast: [playPodcast] có metadata, phát tiếp từ vị trí đã lưu, tốc độ, tua ±10s,
 *    và TỰ ĐỘNG lưu vị trí nghe vào Room (định kỳ 5s khi đang phát + khi pause/tua/dừng/hết).
 *
 * Việc lưu tiến độ nằm ngay tại singleton này thay vì trong ViewModel vì phiên phát
 * sống lâu hơn màn hình (mini-player chạy trên mọi tab) — ViewModel bị clear thì
 * tiến độ vẫn được ghi tiếp. Chỉ các mediaId của podcast mới được ghi DB.
 */
@androidx.media3.common.util.UnstableApi
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val podcastEpisodeDao: PodcastEpisodeDao,
    private val podcastDownloadManager: PodcastDownloadManager,
    private val simpleCache: SimpleCache
) {
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    // ── StateFlows cho UI ───────────────────────────────────────────────────────

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

    /** mediaId hiện tại: "surah:ayah" với Quran, hoặc episodeId với podcast. */
    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId = _currentMediaId.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(DEFAULT_SPEED)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _nowPlayingTitle = MutableStateFlow<String?>(null)
    val nowPlayingTitle = _nowPlayingTitle.asStateFlow()

    private val _nowPlayingArtist = MutableStateFlow<String?>(null)
    val nowPlayingArtist = _nowPlayingArtist.asStateFlow()

    private val _nowPlayingArtworkPath = MutableStateFlow<String?>(null)
    val nowPlayingArtworkPath = _nowPlayingArtworkPath.asStateFlow()

    // ── Trạng thái nội bộ ───────────────────────────────────────────────────────

    private var onPlaybackFinished: (() -> Unit)? = null

    /** true nếu phiên phát hiện tại là podcast (mới được tự động lưu tiến độ vào Room). */
    private var isPodcastSession = false
    private var lastProgressSaveElapsedMs = 0L

    init {
        startTicker()
    }

    // ── Public API: Quran (giữ nguyên hành vi cũ nhưng bổ sung metadata) ───────────────────────────────

    fun play(
        url: String,
        mediaId: String,
        title: String? = null,
        artist: String? = null,
        artworkPath: String? = "icon/quran.png",
        onFinished: (() -> Unit)? = null
    ) {
        startPlayback(
            url = url,
            mediaId = mediaId,
            startPositionMs = 0L,
            title = title,
            artist = artist,
            artworkPath = artworkPath,
            isPodcast = false,
            onFinished = onFinished
        )
    }

    /** Phát danh sách nhiều mục (vd: toàn bộ Surah hoặc Podcast playlist) để đạt Gapless Playback. */
    fun playList(
        items: List<AudioPlayItem>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
        isPodcast: Boolean = false,
        playWhenReady: Boolean = true
    ) {
        val validItems = items.filter { it.url.isNotBlank() }
        if (validItems.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, validItems.size - 1)

        initializePlayer()
        onPlaybackFinished = null
        if (isPodcastSession && _currentMediaId.value != null) persistProgressNow()

        isPodcastSession = isPodcast
        val firstItem = validItems.getOrNull(safeIndex)
        _currentMediaId.value = firstItem?.mediaId
        _nowPlayingTitle.value = firstItem?.title
        _nowPlayingArtist.value = firstItem?.artist
        _nowPlayingArtworkPath.value = firstItem?.artworkPath?.let { path ->
            if (path.startsWith("http") || path.startsWith("file")) path
            else "file:///android_asset/${path.trimStart('/')}"
        }

        val mediaItems = validItems.map { item ->
            val artworkUri = item.artworkPath?.let { path ->
                if (path.startsWith("http")) Uri.parse(path)
                else Uri.parse("file:///android_asset/${path.trimStart('/')}")
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setArtworkUri(artworkUri)
                .build()
            
            val effectiveUrl = getEffectiveAudioUrl(item.url, item.mediaId)
            MediaItem.Builder()
                .setMediaId(item.mediaId)
                .setUri(effectiveUrl.toAudioUri())
                .setMediaMetadata(metadata)
                .build()
        }

        exoPlayer?.apply {
            stop() // Dừng hẳn và xoá buffer cũ để nạp URL mới hoàn toàn
            clearMediaItems()
            setMediaItems(mediaItems, safeIndex, startPositionMs)
            setPlaybackSpeed(_playbackSpeed.value)
            prepare()
            this.playWhenReady = playWhenReady
        }
    }

    // ── Public API: Podcast ─────────────────────────────────────────────────────

    fun playPodcast(
        url: String,
        mediaId: String,
        startPositionMs: Long = 0L,
        title: String? = null,
        artist: String? = null,
        artworkPath: String? = null,
        onFinished: (() -> Unit)? = null
    ) {
        startPlayback(
            url = url,
            mediaId = mediaId,
            startPositionMs = startPositionMs,
            title = title,
            artist = artist,
            artworkPath = artworkPath,
            isPodcast = true,
            onFinished = onFinished
        )
    }

    /** Bước tua nhanh tới/lui (ms). */
    fun seekForward() {
        val player = exoPlayer ?: return
        seekToInternal(player.currentPosition + SKIP_MS)
    }

    fun seekBackward() {
        val player = exoPlayer ?: return
        seekToInternal(player.currentPosition - SKIP_MS)
    }

    fun seekTo(positionMs: Long) = seekToInternal(positionMs)

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 2.5f)
        exoPlayer?.setPlaybackSpeed(_playbackSpeed.value)
    }

    /** Qua vòng tiếp theo trong danh sách tốc độ; trả về tốc độ vừa áp dụng. */
    fun cyclePlaybackSpeed(): Float {
        val current = PLAYBACK_SPEEDS.indexOfFirst { it == _playbackSpeed.value }
        val next = PLAYBACK_SPEEDS[(current + 1).mod(PLAYBACK_SPEEDS.size)]
        setPlaybackSpeed(next)
        return next
    }

    fun skipToNext() {
        exoPlayer?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        exoPlayer?.seekToPreviousMediaItem()
    }

    fun pause() {
        exoPlayer?.pause()
        if (isPodcastSession) persistProgressNow()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun stop() {
        if (isPodcastSession) persistProgressNow()
        exoPlayer?.stop()
        clearNowPlaying()
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    fun getMediaSession(): MediaSession? = mediaSession

    /**
     * Giải phóng player. Nếu một phiên PODCAST đang hoạt động thì bỏ qua — tránh
     * trường hợp ViewModel tính năng khác (Quran) bị xoá và giết luôn nhạc podcast.
     */
    fun release() {
        if (isPodcastSession && _currentMediaId.value != null) return
        releaseSession()
        exoPlayer?.release()
        exoPlayer = null
        clearNowPlaying()
    }

    fun releaseSession() {
        mediaSession?.release()
        mediaSession = null
    }

    // ── Nội bộ ──────────────────────────────────────────────────────────────────

    private fun startPlayback(
        url: String,
        mediaId: String,
        startPositionMs: Long,
        title: String?,
        artist: String?,
        artworkPath: String?,
        isPodcast: Boolean,
        onFinished: (() -> Unit)?
    ) {
        initializePlayer()
        // Chuyển tập: flush vị trí tập podcast cũ TRƯỚC khi ghi đè trạng thái.
        if (isPodcastSession && _currentMediaId.value != null && _currentMediaId.value != mediaId) {
            persistProgressNow()
        }

        onPlaybackFinished = onFinished
        isPodcastSession = isPodcast
        lastProgressSaveElapsedMs = SystemClock.elapsedRealtime()
        _currentMediaId.value = mediaId
        _positionMs.value = startPositionMs
        _durationMs.value = 0L
        _nowPlayingTitle.value = title
        _nowPlayingArtist.value = artist
        _nowPlayingArtworkPath.value = artworkPath?.let { path ->
            if (path.startsWith("http") || path.startsWith("file")) path
            else "file:///android_asset/${path.trimStart('/')}"
        }

        exoPlayer?.apply {
            val artworkUri = artworkPath?.let { path ->
                if (path.startsWith("http")) Uri.parse(path)
                else Uri.parse("file:///android_asset/${path.trimStart('/')}")
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUri)
                .build()
            val effectiveUrl = getEffectiveAudioUrl(url, mediaId)
            setMediaItem(
                MediaItem.Builder()
                    .setMediaId(mediaId)
                    .setUri(effectiveUrl.toAudioUri())
                    .setMediaMetadata(metadata)
                    .build()
            )
            if (startPositionMs > 0L) seekTo(startPositionMs)
            setPlaybackSpeed(_playbackSpeed.value)
            prepare()
            playWhenReady = true
        }
    }

    private fun getEffectiveAudioUrl(url: String, mediaId: String?): String {
        if (mediaId.isNullOrBlank()) return url
        runCatching {
            val localFile = podcastDownloadManager.getDownloadedFile(mediaId)
            if (localFile.exists() && localFile.length() > 0) {
                return localFile.absolutePath
            }
        }
        return url
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
                .build().apply {
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentMediaId.value = mediaItem?.mediaId
                        _nowPlayingTitle.value = mediaItem?.mediaMetadata?.title?.toString()
                        _nowPlayingArtist.value = mediaItem?.mediaMetadata?.artist?.toString()
                        _nowPlayingArtworkPath.value = mediaItem?.mediaMetadata?.artworkUri?.toString()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (!isPlaying && isPodcastSession) persistProgressNow()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isBuffering.value = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_ENDED) {
                            // Hết tập podcast -> reset vị trí đã lưu để lần sau nghe lại từ đầu.
                            if (isPodcastSession) persistProgressAt(0L)
                            _positionMs.value = exoPlayer?.duration ?: 0L
                            onPlaybackFinished?.invoke()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player Error: ${error.message}", error)
                        _isPlaying.value = false
                        _isBuffering.value = false
                    }
                })
            }
            mediaSession?.release()
            mediaSession = MediaSession.Builder(context, exoPlayer!!)
                .setId("AudioPlayerManagerSession")
                .build()
        }
    }

    /** Vòng lặp 500ms cập nhật position/duration cho UI; mỗi 5s lưu tiến độ podcast. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = mainScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val player = exoPlayer ?: continue
                if (_currentMediaId.value == null) continue

                try {
                    // Kiểm tra trạng thái player trước khi truy cập các thuộc tính nhạy cảm
                    if (player.playbackState != Player.STATE_IDLE) {
                        val duration = player.duration
                        if (duration > 0) _durationMs.value = duration
                        _positionMs.value = player.currentPosition

                        if (isPodcastSession && _isPlaying.value) {
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastProgressSaveElapsedMs >= PROGRESS_SAVE_INTERVAL_MS) {
                                lastProgressSaveElapsedMs = now
                                persistProgressNow()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Có thể player đã bị release hoặc đang trong trạng thái lỗi
                    Log.w(TAG, "Ticker error: ${e.message}")
                }
            }
        }
    }

    private fun seekToInternal(targetMs: Long) {
        val player = exoPlayer ?: return
        val clamped = targetMs.coerceIn(0L, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        player.seekTo(clamped)
        _positionMs.value = clamped
        if (isPodcastSession) persistProgressAt(clamped)
    }

    /** Ghi vị trí HIỆN TẠI của tập đang phát xuống Room (bất đồng bộ, an toàn lỗi). */
    private fun persistProgressNow() {
        persistProgressAt(_positionMs.value)
    }

    private fun persistProgressAt(positionOverride: Long) {
        val episodeId = _currentMediaId.value ?: return
        if (!isPodcastSession) return
        val position = positionOverride.coerceAtLeast(0L)
        mainScope.launch(Dispatchers.IO) {
            runCatching { podcastEpisodeDao.updateLastPosition(episodeId, position) }
                .onFailure { Log.w(TAG, "Không lưu được vị trí phát ($episodeId)", it) }
        }
    }

    private fun String.toAudioUri(): Uri {
        val trimmed = this.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://") || trimmed.startsWith("content://")) {
            Uri.parse(trimmed)
        } else if (trimmed.isNotBlank()) {
            Uri.fromFile(java.io.File(trimmed))
        } else {
            Uri.EMPTY
        }
    }

    private fun clearNowPlaying() {
        _currentMediaId.value = null
        _isPlaying.value = false
        _isBuffering.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        _nowPlayingTitle.value = null
        _nowPlayingArtist.value = null
        _nowPlayingArtworkPath.value = null
        onPlaybackFinished = null
        isPodcastSession = false
    }

    companion object {
        private const val TAG = "AudioPlayerManager"
        const val SKIP_MS = 10_000L
        val PLAYBACK_SPEEDS = listOf(0.8f, 1.0f, 1.25f, 1.5f, 2.0f)
        private const val DEFAULT_SPEED = 1.0f
        private const val TICK_INTERVAL_MS = 50L // Giảm từ 500ms xuống 50ms để highlight mượt mà
        private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
    }
}
