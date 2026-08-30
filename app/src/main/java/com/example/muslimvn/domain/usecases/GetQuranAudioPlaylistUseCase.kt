package com.example.muslimvn.domain.usecases

import com.example.muslimvn.data.util.AudioPlayItem
import com.example.muslimvn.data.util.QuranAudioUrlBuilder
import com.example.muslimvn.domain.models.availableReciters
import com.example.muslimvn.domain.repository.QuranRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetQuranAudioPlaylistUseCase @Inject constructor(
    private val quranRepository: QuranRepository
) {
    suspend operator fun invoke(
        surahNumber: Int,
        reciterIdentifier: String
    ): List<AudioPlayItem> {
        val surah = quranRepository.getSurahByNumber(surahNumber) ?: return emptyList()
        val reciter = availableReciters.find { it.identifier == reciterIdentifier } 
            ?: availableReciters[0]

        // Đảm bảo có timing để lấy Audio URL chuẩn từ Quran.com
        val allTimings = quranRepository.fetchAndCacheSurahTiming(surahNumber, reciter.quranComId)
        val ayahs = quranRepository.getAyahsBySurah(surahNumber).first()

        return ayahs.map { ayah ->
            val mediaId = "${surah.number}:${ayah.ayahNumber}"
            
            // 1. Kiểm tra file local (đã tải offline)
            val localPath = quranRepository.getDownloadedAyahPathSync(mediaId, reciter.quranComId)
            
            // 2. Nếu không có local, dùng Audio URL từ timing chuẩn (Quran.com)
            val timing = allTimings.find { it.verseKey == mediaId }
            val audioUrl = localPath ?: timing?.audioUrl ?: QuranAudioUrlBuilder.buildAyahUrl(surah.number, ayah.ayahNumber, reciterIdentifier)
            
            AudioPlayItem(
                url = audioUrl,
                mediaId = mediaId,
                title = "${surah.nameVietnamese} - Câu ${ayah.ayahNumber}",
                artist = reciter.name,
                artworkPath = reciter.imageUrl
            )
        }
    }
}
