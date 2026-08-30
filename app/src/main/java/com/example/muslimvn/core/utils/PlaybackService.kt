package com.example.muslimvn.core.utils

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.muslimvn.data.util.AudioPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return audioPlayerManager.getMediaSession()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = audioPlayerManager.getExoPlayer()
        if (player != null && (!player.playWhenReady || player.mediaItemCount == 0)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioPlayerManager.releaseSession()
        super.onDestroy()
    }
}
