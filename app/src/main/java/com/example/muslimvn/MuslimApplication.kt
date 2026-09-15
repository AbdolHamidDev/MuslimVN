package com.example.muslimvn

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.muslimvn.core.network.NewPipeDownloader
import com.example.muslimvn.domain.repository.PodcastRepository
import com.example.muslimvn.domain.repository.QuranRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.ContentCountry
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class MuslimApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var quranRepository: QuranRepository

    @Inject
    lateinit var podcastRepository: PodcastRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo NewPipe Extractor với ngôn ngữ Tiếng Việt và Quốc gia Việt Nam
        NewPipe.init(
            NewPipeDownloader(okHttpClient), 
            Localization("vi", "VN"),
            ContentCountry("VN")
        )

        // Seed dữ liệu cục bộ một lần lúc khởi động (no-op nếu DB đã có sẵn).
        CoroutineScope(Dispatchers.IO).launch {
            quranRepository.initializeData()
            podcastRepository.initializeData()
        }
    }
}
