package com.example.muslimvn.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
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
        seedMutex.withLock {
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

    override fun getEpisodesByScholarPaging(scholarId: String): Flow<PagingData<PodcastEpisode>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { episodeDao.getEpisodesByScholarPaging(scholarId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

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

    override suspend fun syncScholarMetadata(scholarId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val scholar = scholarDao.getScholarById(scholarId)
            ?: return@withContext Result.failure(IllegalArgumentException("Scholar $scholarId not found"))

        rssParser.fetchChannelMetadata(scholar.rssUrl).mapCatching { meta ->
            val updated = scholar.copy(
                name = meta.title.ifEmpty { scholar.name },
                bio = meta.description.ifEmpty { scholar.bio },
                avatarPath = if (scholar.avatarPath.startsWith("images/")) scholar.avatarPath
                             else (meta.imageUrl ?: scholar.avatarPath)
            )
            scholarDao.insertScholars(listOf(updated))
        }
    }

    override suspend fun syncMuslimCentralDirectory(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val existingMap = scholarDao.getAllScholarsOnce().associateBy { it.id }
            val newEntities = mutableListOf<ScholarEntity>()

            MUSLIM_CENTRAL_DIRECTORY_SLUGS.forEach { slug ->
                val rssUrl = "https://rss.muslimcentral.com/$slug.rss"
                val existing = existingMap[slug]

                if (existing == null) {
                    // Học giả mới chưa có trong DB -> Fetch channel metadata từ xa
                    rssParser.fetchChannelMetadata(rssUrl).onSuccess { meta ->
                        if (meta.title.isNotBlank()) {
                            val newEntity = ScholarEntity(
                                id = slug,
                                name = meta.title,
                                title = "Học Giả Muslim Central",
                                bio = meta.description.ifEmpty { "Học giả thuộc hệ thống phát thanh Muslim Central." },
                                avatarPath = meta.imageUrl ?: "https://artwork.muslimcentral.com/$slug.jpg",
                                rssUrl = rssUrl,
                                tags = inferScholarTags(meta.title, meta.description),
                                featured = false
                            )
                            newEntities.add(newEntity)
                            // Insert từng học giả mới vào Room để Flow phát ngay dữ liệu mới ra UI
                            scholarDao.insertScholars(listOf(newEntity))
                        }
                    }
                } else {
                    // Học giả đã có -> Cập nhật thông tin/ảnh từ xa nếu avatarPath là URL HTTP
                    rssParser.fetchChannelMetadata(rssUrl).onSuccess { meta ->
                        if (meta.title.isNotBlank()) {
                            val updated = existing.copy(
                                name = meta.title.ifEmpty { existing.name },
                                bio = meta.description.ifEmpty { existing.bio },
                                avatarPath = if (existing.avatarPath.startsWith("images/")) existing.avatarPath
                                             else (meta.imageUrl ?: existing.avatarPath)
                            )
                            scholarDao.insertScholars(listOf(updated))
                        }
                    }
                }
            }
            newEntities.size
        }.onFailure { error ->
            Log.e(TAG, "Lỗi đồng bộ Muslim Central Directory", error)
        }
    }

    private fun inferScholarTags(title: String, bio: String): List<String> {
        val text = "$title $bio".lowercase()
        val tags = mutableListOf<String>()

        if (text.contains("tafsir") || text.contains("quran") || text.contains("koran") || text.contains("recitation")) {
            tags.add("tafsir")
        }
        if (text.contains("fiqh") || text.contains("law") || text.contains("jurisprudence") || text.contains("hajj") || text.contains("fatwa")) {
            tags.add("fiqh")
        }
        if (text.contains("aqidah") || text.contains("creed") || text.contains("theology") || text.contains("belief")) {
            tags.add("aqidah")
        }
        if (text.contains("tazkiyah") || text.contains("heart") || text.contains("spiritual") || text.contains("ethics") || text.contains("character")) {
            tags.add("tazkiyah")
        }
        if (text.contains("contemporary") || text.contains("modern") || text.contains("youth") || text.contains("social") || text.contains("society")) {
            tags.add("contemporary")
        }
        if (tags.isEmpty() || text.contains("inspiration") || text.contains("dawah") || text.contains("speaker") || text.contains("motivational")) {
            tags.add("inspiration")
        }
        return tags.distinct()
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

        /** Mở rộng danh mục slug hơn 70+ học giả Muslim Central được tự động phát hiện và đồng bộ */
        private val MUSLIM_CENTRAL_DIRECTORY_SLUGS = listOf(
            // Core Scholars
            "mufti-menk", "nouman-ali-khan", "omar-suleiman", "yasir-qadhi",
            "bilal-philips", "hamza-yusuf", "abdul-nasir-jangda", "wahaj-tarin",
            "mohamed-hoblos", "haifaa-younis", "abdulbary-yahya", "zahir-mahmood",
            "taimiyyah-zubair", "hasan-ali", "shady-alsuleiman", "moutasem-al-hameedy",
            "muhammad-west", "muiz-bukhary",

            // Global Muslim Central Scholars & Preachers
            "zakir-naik", "majed-mahmoud", "siraj-wahhaj", "khalid-yasin",
            "yusuf-estes", "ahmed-deedat", "zaid-shakir", "yaser-birjas",
            "ismail-kamdar", "saajid-lipham", "mikaeel-smith", "abdullah-hakim-quick",
            "sulaiman-moola", "usama-canon", "yahya-ibrahim", "zohra-sarwari",
            "hussain-yee", "hassan-elwan", "tariq-appleby", "assim-al-hakeem",
            "abdur-raheem-green", "suhaib-webb", "waleed-basyouni", "ammar-al-shukry",
            "hasib-noor", "ammar-nakshawani", "yusha-evans", "toure-roberts",
            "tawfique-chowdhury", "tariq-ramadan", "salman-al-odah", "sajid-umar",
            "saad-tasleem", "muhammad-salah", "muhammad-al-shareef", "muhammad-al-yaqoubi",
            "khalid-green", "kamal-el-zant", "ismail-london", "idrees-zubair",
            "hussain-kamani", "hisham-al-awadi", "hatem-al-haj", "hamza-tzortzis",
            "habib-ali-al-jifri", "fariq-naik", "ebrahim-bham", "daood-butt",
            "boonaa-mohammed", "azhar-iqbal", "asif-uddin", "alaa-elsayed",
            "adnan-rashid", "abdur-rahman-ibn-yusuf", "abdullah-oduro", "abdullah-hashem",
            "abdul-wahab-saleem", "abdul-aziz-suraqah", "abdelrahman-murphy"
        )
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
