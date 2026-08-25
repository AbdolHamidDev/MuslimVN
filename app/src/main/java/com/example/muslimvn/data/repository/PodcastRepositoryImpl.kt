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

    override suspend fun initializeData() {
        if (scholarDao.count() > 0) return
        seedMutex.withLock {
            if (scholarDao.count() > 0) return
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
        }
    }

    private suspend fun loadScholarsFile(): ScholarsFileDto? = withContext(Dispatchers.IO) {
        cachedFile?.let { return@withContext it }
        runCatching {
            context.assets.open(ASSET_FILE).bufferedReader().use { reader ->
                gson.fromJson(reader, ScholarsFileDto::class.java)
            }
        }.getOrElse { null }?.also { cachedFile = it }
    }

    override fun getScholars(): Flow<List<Scholar>> =
        scholarDao.getAllScholars().map { list -> list.map { it.toDomain() } }

    override suspend fun getScholarById(id: String): Scholar? =
        scholarDao.getScholarById(id)?.toDomain()

    override suspend fun getCategories(): List<PodcastCategory> =
        loadScholarsFile()?.categories?.map { PodcastCategory(it.id, it.name) } ?: emptyList()

    override fun getEpisodesByScholar(scholarId: String): Flow<List<PodcastEpisode>> =
        episodeDao.getEpisodesByScholar(scholarId).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshEpisodes(scholarId: String): Result<Int> {
        val scholar = scholarDao.getScholarById(scholarId)
            ?: return Result.failure(IllegalStateException("Scholar not found: $scholarId"))

        return rssParser.fetchEpisodes(scholar.rssUrl).mapCatching { rssEpisodes ->
            if (rssEpisodes.isEmpty()) return@mapCatching 0
            val existingById = episodeDao.getEpisodesByScholarOnce(scholarId).associateBy { it.id }
            val merged = rssEpisodes.map { rss ->
                val old = existingById[rss.id]
                PodcastEpisodeEntity(
                    id = rss.id,
                    scholarId = scholarId,
                    title = rss.title,
                    audioUrl = rss.audioUrl,
                    artworkUrl = rss.artworkUrl,
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
    }

    private fun ScholarEntity.toDomain() = Scholar(
        id, name, title, bio, avatarPath, rssUrl, tags, featured
    )

    private fun PodcastEpisodeEntity.toDomain() = PodcastEpisode(
        id, scholarId, title, audioUrl, artworkUrl, duration, pubDate, description, isDownloaded, lastPositionMs
    )

    companion object {
        private const val TAG = "PodcastRepository"
        private const val ASSET_FILE = "scholars.json"
    }
}

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
