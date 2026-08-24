package com.example.muslimvn

import android.app.Application
import com.example.muslimvn.domain.repository.PodcastRepository
import com.example.muslimvn.domain.repository.QuranRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MuslimApplication : Application() {

    @Inject
    lateinit var quranRepository: QuranRepository

    @Inject
    lateinit var podcastRepository: PodcastRepository

    override fun onCreate() {
        super.onCreate()
        // Seed dữ liệu cục bộ một lần lúc khởi động (no-op nếu DB đã có sẵn).
        CoroutineScope(Dispatchers.IO).launch {
            quranRepository.initializeData()
            podcastRepository.initializeData()
        }
    }
}
