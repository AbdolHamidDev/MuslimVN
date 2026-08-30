package com.example.muslimvn.data.util

import com.example.muslimvn.data.preferences.QuranPreferences
import com.example.muslimvn.domain.usecases.GetQuranAudioPlaylistUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Điều phối viên cho trình phát Quran:
 * - Theo dõi thay đổi Qari (Reciter) từ Preferences.
 * - Nếu đang phát Quran, tự động dừng và nạp lại playlist với giọng mới.
 * - Đảm bảo tính đồng bộ giữa các màn hình (Settings, QuranScreen, SurahDetail).
 */
@androidx.media3.common.util.UnstableApi
@Singleton
class QuranPlayerCoordinator @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager,
    private val quranPreferences: QuranPreferences,
    private val getQuranAudioPlaylistUseCase: GetQuranAudioPlaylistUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        observeReciterChanges()
    }

    private fun observeReciterChanges() {
        scope.launch {
            quranPreferences.reciterIdentifier
                .distinctUntilChanged()
                .drop(1) // Bỏ qua giá trị khởi tạo khi vừa mở app
                .collect { reciterIdentifier ->
                    reloadIfQuranPlaying(reciterIdentifier)
                }
        }
    }

    private suspend fun reloadIfQuranPlaying(reciterIdentifier: String) {
        val currentId = audioPlayerManager.currentMediaId.value ?: return
        
        // Chỉ xử lý nếu đang phát Quran (format "surahNumber:ayahNumber")
        if (currentId.contains(":")) {
            val wasPlaying = audioPlayerManager.isPlaying.value
            val parts = currentId.split(":")
            val surahNumber = parts[0].toIntOrNull() ?: return
            val ayahNumber = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1

            // 1. Dừng ngay giọng cũ
            audioPlayerManager.stop()

            // 2. Nạp playlist mới với reciter mới
            val newPlaylist = getQuranAudioPlaylistUseCase(surahNumber, reciterIdentifier)
            if (newPlaylist.isNotEmpty()) {
                audioPlayerManager.playList(
                    items = newPlaylist,
                    startIndex = ayahNumber - 1,
                    playWhenReady = wasPlaying
                )
            }
        }
    }
}
