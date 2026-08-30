package com.example.muslimvn.data.repository

import android.content.Context
import androidx.work.*
import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.entities.AyahEntity
import com.example.muslimvn.data.local.entities.SurahEntity
import com.example.muslimvn.data.local.entities.TafsirEntity
import com.example.muslimvn.data.local.entities.VerseTimingEntity
import com.example.muslimvn.data.remote.QuranApiService
import com.example.muslimvn.data.util.QuranJsonParser
import com.example.muslimvn.data.util.TafsirTranslator
import com.example.muslimvn.data.workers.MushafDownloadWorker
import com.example.muslimvn.data.workers.QuranDownloadWorker
import com.example.muslimvn.domain.models.*
import com.example.muslimvn.domain.repository.QuranRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

data class MushafPageMapping(
    val page: Int,
    val start: String,
    val end: String,
    val ayahs: List<String>
)

class QuranRepositoryImpl @Inject constructor(
    private val dao: QuranDao,
    private val apiService: QuranApiService,
    private val jsonParser: QuranJsonParser,
    private val translator: TafsirTranslator,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : QuranRepository {

    private var pagesMapping: List<MushafPageMapping>? = null
    private var coordinatesMap: Map<String, List<List<Int>>>? = null

    private suspend fun getPagesMapping(): List<MushafPageMapping> = withContext(Dispatchers.IO) {
        val currentMapping = pagesMapping
        if (currentMapping != null) return@withContext currentMapping

        try {
            val jsonString = context.assets.open("quran_pages_mapping.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<MushafPageMapping>>() {}.type
            val mapping: List<MushafPageMapping>? = gson.fromJson(jsonString, type)
            if (mapping != null) {
                pagesMapping = mapping
                mapping
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getSurahs(query: String): Flow<List<Surah>> {
        return if (query.isBlank()) {
            dao.getAllSurahs().map { entities -> entities.map { it.toDomain() } }
        } else {
            dao.searchSurahs(query).map { entities -> entities.map { it.toDomain() } }
        }
    }

    override fun getAyahsBySurah(surahId: Int): Flow<List<Ayah>> {
        return dao.getAyahsBySurah(surahId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getSurahByNumber(number: Int): Surah? {
        return dao.getSurahByNumber(number)?.toDomain()
    }

    override suspend fun toggleBookmark(ayahId: Int, isBookmarked: Boolean) {
        dao.toggleBookmark(ayahId, isBookmarked)
    }

    override fun getBookmarkedAyahs(): Flow<List<Ayah>> {
        return dao.getBookmarkedAyahs().map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchAyahs(query: String): Flow<List<Ayah>> {
        return dao.searchAyahs(query).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getVerseTiming(verseKey: String, recitationId: Int): VerseTiming? {
        // 1. Check local cache first
        val cached = dao.getVerseTiming(verseKey, recitationId)
        if (cached != null) {
            val type = object : TypeToken<List<WordSegment>>() {}.type
            val segments: List<WordSegment> = gson.fromJson(cached.segmentsJson, type)
            return VerseTiming(verseKey, segments, cached.audioUrl)
        }

        // 2. Fetch from remote if not cached (Batch fetch for the whole surah)
        val surahNumber = verseKey.split(":")[0].toIntOrNull() ?: return null
        val timings = fetchAndCacheSurahTiming(surahNumber, recitationId)
        return timings.find { it.verseKey == verseKey }
    }

    override suspend fun fetchAndCacheSurahTiming(surahNumber: Int, recitationId: Int): List<VerseTiming> {
        return try {
            val response = apiService.getChapterWithAudio(surahNumber, recitationId = recitationId)
            response.verses.map { verse ->
                val audioData = verse.audio
                val rawAudioUrl = audioData.url
                val audioUrl = if (rawAudioUrl.startsWith("http")) {
                    rawAudioUrl
                } else {
                    "https://audio.qurancdn.com/$rawAudioUrl"
                }

                val segments = audioData.segments.mapNotNull { segment ->
                    if (segment.size < 4) return@mapNotNull null
                    WordSegment(
                        wordIndex = segment[0].toInt(),
                        startTimeMs = segment[2].toLong(),
                        endTimeMs = segment[3].toLong()
                    )
                }

                // Save to cache
                val entity = VerseTimingEntity(
                    verseKey = verse.verseKey,
                    reciterId = recitationId,
                    segmentsJson = gson.toJson(segments),
                    audioUrl = audioUrl
                )
                dao.insertVerseTiming(entity)

                VerseTiming(verse.verseKey, segments, audioUrl)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun prefetchSurahTiming(surahNumber: Int, recitationId: Int, onProgress: (Float) -> Unit) {
        onProgress(0.1f)
        fetchAndCacheSurahTiming(surahNumber, recitationId)
        onProgress(1.0f)
    }

    override suspend fun getTafsir(verseKey: String, resourceId: Int): Tafsir? {
        val cached = dao.getTafsir(verseKey, resourceId)
        if (cached != null) {
            return Tafsir(cached.verseKey, cached.resourceId, cached.text, cached.translatedText)
        }

        return try {
            val response = apiService.getTafsir(resourceId, verseKey)
            val entity = TafsirEntity(
                verseKey = verseKey,
                resourceId = resourceId,
                text = response.tafsir.text
            )
            dao.insertTafsir(entity)
            Tafsir(entity.verseKey, entity.resourceId, entity.text)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun translateTafsir(verseKey: String, text: String): String? {
        // 1. Dịch văn bản
        val translated = translator.translate(text) ?: return null
        
        // 2. Cập nhật vào DB để lần sau dùng luôn
        val cached = dao.getTafsir(verseKey, 169)
        if (cached != null) {
            dao.insertTafsir(cached.copy(translatedText = translated))
        }
        
        return translated
    }

    override suspend fun initializeData() {
        val existingSurahs = dao.getAllSurahs().first()
        if (existingSurahs.isEmpty()) {
            val (surahs, ayahs) = jsonParser.parseQuranData()
            dao.insertSurahs(surahs)
            dao.insertAyahs(ayahs)
        }
    }

    override fun getDownloadedAyahPath(verseKey: String, reciterId: Int): Flow<String?> {
        return dao.getDownloadedAyahPathFlow(verseKey, reciterId)
    }

    override suspend fun getDownloadedAyahPathSync(verseKey: String, reciterId: Int): String? {
        return dao.getDownloadedAyah(verseKey, reciterId)?.localPath
    }

    override fun getDownloadedAyahsCount(surahNumber: Int, reciterId: Int): Flow<Int> {
        return dao.getDownloadedAyahsCountFlow(surahNumber, reciterId)
    }

    override fun startSurahDownload(surahNumber: Int, reciterId: Int) {
        val workRequest = OneTimeWorkRequestBuilder<QuranDownloadWorker>()
            .setInputData(workDataOf("surahNumber" to surahNumber, "reciterId" to reciterId))
            .addTag("download_surah_${surahNumber}_$reciterId")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_surah_${surahNumber}_$reciterId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun getFullQuranDownloadProgress(reciterId: Int): Flow<Float?> {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("download_full_quran_$reciterId")

        return combine(
            workInfos,
            dao.getTotalDownloadedAyahsCount(reciterId)
        ) { infos: List<WorkInfo>, count: Int ->
            val info = infos.firstOrNull()
            if (info != null && info.state == WorkInfo.State.RUNNING) {
                info.progress.getFloat("progress", count.toFloat() / 6236f)
            } else {
                if (count > 0) count.toFloat() / 6236f else null
            }
        }
    }

    override fun startFullQuranDownload(reciterId: Int, reciterName: String) {
        val workRequest = OneTimeWorkRequestBuilder<QuranDownloadWorker>()
            .setInputData(workDataOf(
                "reciterId" to reciterId,
                "surahNumber" to -1,
                "reciterName" to reciterName
            ))
            .addTag("download_full_quran_$reciterId")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_full_quran_$reciterId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun pauseDownload(reciterId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("download_full_quran_$reciterId")
    }

    override fun clearDownloadedAudio(reciterId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("download_full_quran_$reciterId")
        val audioDir = File(context.filesDir, "audio/$reciterId")
        if (audioDir.exists()) {
            audioDir.deleteRecursively()
        }
        // Also clear from DB
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            dao.deleteAllDownloadedAyahs(reciterId)
        }
    }

    override fun clearSurahAudio(surahNumber: Int, reciterId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("download_surah_${surahNumber}_$reciterId")
        val surahDir = File(context.filesDir, "audio/$reciterId/$surahNumber")
        if (surahDir.exists()) {
            surahDir.deleteRecursively()
        }
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            dao.deleteSurahDownloadedAyahs(surahNumber, reciterId)
        }
    }

    override fun getDownloadStatus(reciterId: Int): Flow<DownloadStatus> {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("download_full_quran_$reciterId")

        return combine(
            workInfos,
            dao.getTotalDownloadedAyahsCount(reciterId)
        ) { infos: List<WorkInfo>, count: Int ->
            val info = infos.firstOrNull()
            when {
                info == null -> {
                    if (count > 0 && count >= 6236) DownloadStatus.COMPLETED else DownloadStatus.IDLE
                }
                info.state == WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
                info.state == WorkInfo.State.ENQUEUED -> DownloadStatus.DOWNLOADING
                info.state == WorkInfo.State.CANCELLED || info.state == WorkInfo.State.FAILED -> {
                    if (count > 0) DownloadStatus.PAUSED else DownloadStatus.IDLE
                }
                info.state == WorkInfo.State.SUCCEEDED -> {
                    if (count >= 6236) DownloadStatus.COMPLETED else DownloadStatus.IDLE
                }
                else -> DownloadStatus.IDLE
            }
        }
    }

    override fun getSurahDownloadStatus(surahNumber: Int, reciterId: Int): Flow<DownloadStatus> {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("download_surah_${surahNumber}_$reciterId")

        return combine(
            workInfos,
            dao.getDownloadedAyahsCountFlow(surahNumber, reciterId),
            getDownloadStatus(reciterId)
        ) { infos: List<WorkInfo>, count: Int, fullStatus: DownloadStatus ->
            // If full quran is downloading or completed, that takes precedence
            if (fullStatus == DownloadStatus.COMPLETED || fullStatus == DownloadStatus.DOWNLOADING) {
                return@combine fullStatus
            }

            val info = infos.firstOrNull()
            val surah = runBlocking { getSurahByNumber(surahNumber) }
            val total = surah?.totalAyahs ?: 0

            when {
                info == null -> {
                    if (total > 0 && count >= total) DownloadStatus.COMPLETED else DownloadStatus.IDLE
                }
                info.state == WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
                info.state == WorkInfo.State.ENQUEUED -> DownloadStatus.DOWNLOADING
                info.state == WorkInfo.State.CANCELLED || info.state == WorkInfo.State.FAILED -> {
                    if (total > 0 && count > 0) DownloadStatus.PAUSED else DownloadStatus.IDLE
                }
                info.state == WorkInfo.State.SUCCEEDED -> {
                    if (total > 0 && count >= total) DownloadStatus.COMPLETED else DownloadStatus.IDLE
                }
                else -> DownloadStatus.IDLE
            }
        }
    }

    override suspend fun getPageForAyah(surah: Int, ayah: Int): Int = try {
        val mapping = getPagesMapping()
        val verseKey = "$surah:$ayah"
        mapping.find { it.ayahs.contains(verseKey) }?.page ?: 1
    } catch (e: Exception) {
        1
    }

    override suspend fun getAyahsForPage(page: Int): List<String> = try {
        val mapping = getPagesMapping()
        mapping.find { it.page == page }?.ayahs ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    override fun startMushafDownload() {
        val workRequest = OneTimeWorkRequestBuilder<MushafDownloadWorker>()
            .addTag("download_mushaf")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_mushaf",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun pauseMushafDownload() {
        WorkManager.getInstance(context).cancelUniqueWork("download_mushaf")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMushafDownloadProgress(): Flow<Float?> {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("download_mushaf")

        return workInfos.flatMapLatest { infos ->
            val info = infos.firstOrNull()
            if (info != null && info.state == WorkInfo.State.RUNNING) {
                flowOf(info.progress.getFloat("progress", 0f))
            } else {
                // Nếu không đang chạy, lấy tỉ lệ file thực tế
                flow<Float?> {
                    val downloadDir = File(context.filesDir, "mushaf")
                    if (!downloadDir.exists()) {
                        emit(null)
                    } else {
                        val count = downloadDir.listFiles()?.size ?: 0
                        if (count > 0) emit(count.toFloat() / 604f) else emit(null)
                    }
                }
            }
        }
    }

    override fun getMushafDownloadStatus(): Flow<DownloadStatus> {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("download_mushaf")

        return combine(
            workInfos,
            isMushafOffline()
        ) { infos: List<WorkInfo>, isOffline: Boolean ->
            if (isOffline) return@combine DownloadStatus.COMPLETED

            val info = infos.firstOrNull()
            val downloadDir = File(context.filesDir, "mushaf")
            val count = if (downloadDir.exists()) downloadDir.listFiles()?.size ?: 0 else 0

            when {
                info == null -> {
                    if (count > 0) DownloadStatus.PAUSED else DownloadStatus.IDLE
                }
                info.state == WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
                info.state == WorkInfo.State.ENQUEUED -> DownloadStatus.DOWNLOADING
                info.state == WorkInfo.State.CANCELLED || info.state == WorkInfo.State.FAILED -> {
                    if (count > 0) DownloadStatus.PAUSED else DownloadStatus.IDLE
                }
                info.state == WorkInfo.State.SUCCEEDED -> {
                    if (count >= 604) DownloadStatus.COMPLETED else DownloadStatus.IDLE
                }
                else -> DownloadStatus.IDLE
            }
        }
    }

    override fun isMushafOffline(): Flow<Boolean> = flow {
        val downloadDir = File(context.filesDir, "mushaf")
        if (!downloadDir.exists()) {
            emit(false)
            return@flow
        }
        val count = downloadDir.listFiles()?.size ?: 0
        emit(count >= 604)
    }.flowOn(Dispatchers.IO)

    override fun clearMushafData() {
        WorkManager.getInstance(context).cancelUniqueWork("download_mushaf")
        val downloadDir = File(context.filesDir, "mushaf")
        if (downloadDir.exists()) {
            downloadDir.deleteRecursively()
        }
    }

    override suspend fun getAyahCoordinates(verseKey: String): List<List<Int>> = withContext(Dispatchers.IO) {
        if (coordinatesMap == null) {
            try {
                val jsonString = context.assets.open("ayah_coordinates.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<Map<String, List<List<Int>>>>() {}.type
                coordinatesMap = gson.fromJson(jsonString, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        coordinatesMap?.get(verseKey) ?: emptyList()
    }

    private fun SurahEntity.toDomain() = Surah(
        number = number,
        nameArabic = nameArabic,
        nameVietnamese = nameVietnamese,
        totalAyahs = totalAyahs,
        revelationType = revelationType
    )

    private fun AyahEntity.toDomain(): Ayah {
        var cleanText = textArabic
        // Nếu không phải Surah 1 (Fatiha) và là câu số 1 -> Loại bỏ Bismillah prefix nếu có
        // để khớp với dữ liệu timing từ Quran.com
        if (surahId != 1 && ayahNumber == 1) {
            val bismillahPrefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "
            if (cleanText.startsWith(bismillahPrefix)) {
                cleanText = cleanText.removePrefix(bismillahPrefix)
            } else if (cleanText.startsWith("بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ")) {
                // Phân biệt một số biến thể unicode nếu có
                cleanText = cleanText.substringAfter("الرَّحِيْمِ").trim()
            }
        }
        
        return Ayah(
            id = id,
            surahId = surahId,
            ayahNumber = ayahNumber,
            textArabic = cleanText,
            textVietnamese = textVietnamese,
            isBookmarked = isBookmarked
        )
    }
}
