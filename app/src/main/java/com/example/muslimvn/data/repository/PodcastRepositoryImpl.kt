package com.example.muslimvn.data.repository

import android.content.Context
import android.util.Log
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import com.example.muslimvn.data.local.dao.ScholarDao
import com.example.muslimvn.data.local.entities.PodcastEpisodeEntity
import com.example.muslimvn.data.local.entities.ScholarEntity
import com.example.muslimvn.data.util.RssParserUtility
import com.example.muslimvn.domain.models.PodcastCategory
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.domain.repository.PodcastRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Podcast OFFLINE-FIRST:
 * 1) Seed học giả từ assets/scholars.json vào Room đúng MỘT lần (mutex + count check).
 * 2) UI đọc học giả / tập phát trực tiếp qua Room Flow — mở màn hình là có dữ liệu ngay.
 * 3) refreshEpisodes() fetch RSS nền, merge giữ nguyên lastPositionMs & isDownloaded rồi upsert.
 */
@Singleton
class PodcastRepositoryImpl @Inject constructor(
    private val scholarDao: ScholarDao,
    private val episodeDao: PodcastEpisodeDao,
    private val rssParser: RssParserUtility,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : PodcastRepository {

    @Volatile
    private var cachedFile: ScholarsFileDto? = null
    private val seedMutex = Mutex()

    // ── Seed từ assets ──────────────────────────────────────────────────────────

    override suspend fun initializeData() {
        if (scholarDao.count() > 0) return
        seedMutex.withLock {
            if (scholarDao.count() > 0) return // thread khác đã seed xong trong lúc chờ khoá
            val file = loadScholarsFile() ?: return
            val featuredIds = file.featuredScholarIds.toSet()
            scholarDao.insertScholars(
                file.scholars.map { dto ->
                    ScholarEntity(
                        id = dto.id,
                        name = dto.name,
                        title = dto.title,
                        bio = dto.bio,
                        avatarPath = dto.avatar,
                        rssUrl = dto.rssUrl,
                        tags = dto.tags,
                        featured = dto.id in featuredIds
                    )
                }
            )
            Log.i(TAG, "Đã seed ${file.scholars.size} học giả vào Room")
        }
    }

    /** Parse asset một lần duy nhất rồi cache bộ nhớ (cả categories dùng chung). */
    private suspend fun loadScholarsFile(): ScholarsFileDto? = withContext(Dispatchers.IO) {
        cachedFile?.let { return@withContext it }
        runCatching {
            context.assets.open(ASSET_FILE).bufferedReader().use { reader ->
                gson.fromJson(reader, ScholarsFileDto::class.java)
            }
        }.getOrElse { error ->
            Log.e(TAG, "Không thể đọc $ASSET_FILE từ assets", error)
            null
        }?.also { cachedFile = it }
    }

    // ── Học giả / phân loại ─────────────────────────────────────────────────────

    override fun getScholars(): Flow<List<Scholar>> =
        scholarDao.getAllScholars().map { list -> list.map { it.toDomain() } }

    override suspend fun getScholarById(id: String): Scholar? =
        scholarDao.getScholarById(id)?.toDomain()

    override suspend fun getCategories(): List<PodcastCategory> =
        loadScholarsFile()?.categories
            ?.map { PodcastCategory(id = it.id, name = it.name) }
            ?: emptyList()

    // ── Tập phát ────────────────────────────────────────────────────────────────

    override fun getEpisodesByScholar(scholarId: String): Flow<List<PodcastEpisode>> =
        episodeDao.getEpisodesByScholar(scholarId).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshEpisodes(scholarId: String): Result<Int> {
        val scholar = scholarDao.getScholarById(scholarId)
            ?: return Result.failure(IllegalStateException("Không tìm thấy học giả: $scholarId"))

        return rssParser.fetchEpisodes(scholar.rssUrl).mapCatching { rssEpisodes ->
            if (rssEpisodes.isEmpty()) return@mapCatching 0
            // Merge: metadata lấy từ feed, nhưng GIỮ vị trí nghe + trạng thái tải của bản cũ.
            val existingById = episodeDao.getEpisodesByScholarOnce(scholarId).associateBy { it.id }
            val merged = rssEpisodes.map { rss ->
                val old = existingById[rss.id]
                PodcastEpisodeEntity(
                    id = rss.id,
                    scholarId = scholarId,
                    title = rss.title,
                    audioUrl = rss.audioUrl,
                    duration = rss.durationMs,
                    pubDate = rss.pubDateMs,
                    description = rss.description,
                    isDownloaded = old?.isDownloaded ?: false,
                    lastPositionMs = old?.lastPositionMs ?: 0L
                )
            }
            episodeDao.insertEpisodes(merged)
            merged.size
        }
    }

    override suspend fun getEpisodeById(episodeId: String): PodcastEpisode? =
        episodeDao.getEpisodeById(episodeId)?.toDomain()

    override suspend fun savePlaybackPosition(episodeId: String, positionMs: Long) {
        runCatching { episodeDao.updateLastPosition(episodeId, positionMs) }
            .onFailure { Log.w(TAG, "Không lưu được vị trí phát của $episodeId", it) }
    }

    // ── Mapping ─────────────────────────────────────────────────────────────────

    private fun ScholarEntity.toDomain() = Scholar(
        id = id,
        name = name,
        title = title,
        bio = bio,
        avatarPath = avatarPath,
        rssUrl = rssUrl,
        tags = tags,
        featured = featured
    )

    private fun PodcastEpisodeEntity.toDomain() = PodcastEpisode(
        id = id,
        scholarId = scholarId,
        title = title,
        audioUrl = audioUrl,
        duration = duration,
        pubDate = pubDate,
        description = description,
        isDownloaded = isDownloaded,
        lastPositionMs = lastPositionMs
    )

    companion object {
        private const val TAG = "PodcastRepository"
        private const val ASSET_FILE = "scholars.json"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DTO phản ánh cấu trúc scholars.json (Gson, giá trị mặc định chống thiếu trường).
// ─────────────────────────────────────────────────────────────────────────────

private data class ScholarsFileDto(
    val categories: List<CategoryDto> = emptyList(),
    @SerializedName("featuredScholarIds") val featuredScholarIds: List<String> = emptyList(),
    val scholars: List<ScholarDto> = emptyList()
)

private data class CategoryDto(val id: String = "", val name: String = "")

private data class ScholarDto(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val bio: String = "",
    val avatar: String = "",
    val rssUrl: String = "",
    val tags: List<String> = emptyList()
)
