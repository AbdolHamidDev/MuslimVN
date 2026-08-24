package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.example.muslimvn.data.util.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel mỏng cho trình phát toàn màn hình: chỉ pass-through trạng thái của
 * [AudioPlayerManager] dùng chung + các lệnh điều khiển. Không giữ state riêng vì
 * phiên phát thuộc về singleton (mini-player vẫn chạy khi rời màn hình).
 */
@HiltViewModel
class PodcastPlayerViewModel @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    val isPlaying = audioPlayerManager.isPlaying
    val isBuffering = audioPlayerManager.isBuffering
    val currentEpisodeId = audioPlayerManager.currentMediaId
    val positionMs = audioPlayerManager.positionMs
    val durationMs = audioPlayerManager.durationMs
    val playbackSpeed = audioPlayerManager.playbackSpeed
    val title = audioPlayerManager.nowPlayingTitle
    val artist = audioPlayerManager.nowPlayingArtist
    val artworkPath = audioPlayerManager.nowPlayingArtworkPath

    fun togglePlayPause() {
        if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
    }

    fun seekForward() = audioPlayerManager.seekForward()

    fun seekBackward() = audioPlayerManager.seekBackward()

    fun seekTo(positionMs: Long) = audioPlayerManager.seekTo(positionMs)

    fun cyclePlaybackSpeed(): Float = audioPlayerManager.cyclePlaybackSpeed()
}
