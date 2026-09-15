package com.example.muslimvn.core.utils

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.YouTubePlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    @Inject
    lateinit var youtubePlayerManager: YouTubePlayerManager

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Ưu tiên session đang phát
        if (youtubePlayerManager.exoPlayer.isPlaying) {
            return youtubePlayerManager.getMediaSession()
        }
        return audioPlayerManager.getMediaSession()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val audioPlayer = audioPlayerManager.getExoPlayer()
        val youtubePlayer = youtubePlayerManager.exoPlayer
        
        val isAudioPlaying = audioPlayer?.playWhenReady == true && audioPlayer.mediaItemCount > 0
        val isYoutubePlaying = youtubePlayer.playWhenReady && youtubePlayer.mediaItemCount > 0

        if (!isAudioPlaying && !isYoutubePlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioPlayerManager.releaseSession()
        super.onDestroy()
    }
}
