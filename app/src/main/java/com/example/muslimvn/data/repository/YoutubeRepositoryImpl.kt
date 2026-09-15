package com.example.muslimvn.data.repository

import com.example.muslimvn.core.utils.TimeUtils
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.models.YoutubeVideoDetail
import com.example.muslimvn.domain.repository.YoutubeRepository
import com.example.muslimvn.data.util.YoutubeRssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.collections.filterIsInstance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRepositoryImpl @Inject constructor(
    private val rssParser: YoutubeRssParser
) : YoutubeRepository {

    private val videoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<List<YoutubeVideo>, Long>>()
    private val playlistExtractors = java.util.concurrent.ConcurrentHashMap<String, org.schabi.newpipe.extractor.playlist.PlaylistExtractor>()
    private val nextPages = java.util.concurrent.ConcurrentHashMap<String, org.schabi.newpipe.extractor.Page>()
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes cache

    override fun getVideosByChannel(channelUrl: String): Flow<Result<List<YoutubeVideo>>> = flow {
        val cached = videoCache[channelUrl]
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.second) < CACHE_DURATION_MS) {
            emit(Result.success(cached.first))
            return@flow
        }

        try {
            val service = ServiceList.YouTube
            val cleanedUrl = channelUrl.substringBefore("/videos")
                .substringBefore("/streams")
                .substringBefore("/shorts")
                .trimEnd('/')

            val localizedChannelUrl = "$cleanedUrl/videos?hl=vi-VN&gl=VN"
            val linkHandler = service.getChannelLHFactory().fromUrl(localizedChannelUrl)
            val extractor = service.getChannelExtractor(linkHandler)
            extractor.fetchPage()

            val rawId = extractor.id
            val channelId = if (rawId.startsWith("UC")) rawId else "UC-lHJZr3Gqxm24_Vd_AJ5Yw"

            val allItems = mutableListOf<InfoItem>()

            val rssResult = rssParser.fetchVideos(channelId)
            val latestVideo = rssResult.getOrNull()?.firstOrNull()
            val latestVideoId = latestVideo?.id ?: "dQw4w9WgXcQ"

            // Ưu tiên tải dữ liệu từ Playlist Uploads vì URL chuẩn sẽ tự động tương thích với locale
            try {
                val playlistId = "UU" + channelId.drop(2)
                val uploadsPlaylistUrl = "https://www.youtube.com/playlist?list=$playlistId&hl=vi-VN&gl=VN"
                val playlistLinkHandler = service.getPlaylistLHFactory().fromUrl(uploadsPlaylistUrl)
                val playlistExtractor = service.getPlaylistExtractor(playlistLinkHandler)
                playlistExtractor.fetchPage()

                val initialPage = playlistExtractor.getInitialPage()
                allItems.addAll(initialPage.items)

                playlistExtractors[channelUrl] = playlistExtractor
                initialPage.nextPage?.let { nextPages[channelUrl] = it } ?: nextPages.remove(channelUrl)
            } catch (e: Exception) {
                // Rơi xuống dùng RSS nếu các Extractor bị thất bại
            }

            val videos = if (allItems.isNotEmpty()) {
                allItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { item ->
                        val url = item.url ?: ""
                        val timestamp = item.uploadDate?.instant?.toEpochMilli() ?: 0L
                        val formattedDate = if (timestamp > 0) {
                            TimeUtils.getRelativeTimeSpanString(timestamp)
                        } else {
                            ""
                        }

                        val videoId = url.substringAfter("v=", "").substringBefore("&")
                        val rawThumbnail = item.thumbnails.maxByOrNull { it.width * it.height }?.url
                            ?: item.thumbnails.lastOrNull()?.url
                            ?: ""
                        val bestThumbnail = if (videoId.isNotEmpty()) {
                            "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                        } else if (rawThumbnail.isNotEmpty()) {
                            rawThumbnail.replace("hqdefault.jpg", "maxresdefault.jpg")
                        } else {
                            ""
                        }

                        YoutubeVideo(
                            id = videoId,
                            title = item.name ?: "Untitled",
                            thumbnailUrl = bestThumbnail,
                            uploaderName = item.uploaderName ?: "",
                            duration = item.duration,
                            viewCount = item.viewCount,
                            uploadDate = formattedDate,
                            videoUrl = url
                        )
                    }
            } else {
                rssResult.getOrNull() ?: emptyList()
            }

            if (videos.isNotEmpty()) {
                videoCache[channelUrl] = Pair(videos, System.currentTimeMillis())
            }

            emit(Result.success(videos))
        } catch (e: Exception) {
            emit(Result.failure(Exception("${e.javaClass.simpleName}: ${e.message}", e)))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun loadMoreVideos(channelUrl: String): Result<List<YoutubeVideo>> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistExtractor = playlistExtractors[channelUrl] ?: return@runCatching emptyList()
            val nextPage = nextPages[channelUrl] ?: return@runCatching emptyList()

            val page = playlistExtractor.getPage(nextPage)
            if (page.nextPage != null) {
                nextPages[channelUrl] = page.nextPage!!
            } else {
                nextPages.remove(channelUrl)
            }

            page.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val url = item.url ?: ""
                    val timestamp = item.uploadDate?.instant?.toEpochMilli() ?: 0L
                    val formattedDate = if (timestamp > 0) {
                        TimeUtils.getRelativeTimeSpanString(timestamp)
                    } else {
                        ""
                    }

                    val videoId = url.substringAfter("v=", "").substringBefore("&")
                    val rawThumbnail = item.thumbnails.maxByOrNull { it.width * it.height }?.url
                        ?: item.thumbnails.lastOrNull()?.url
                        ?: ""
                    val bestThumbnail = if (videoId.isNotEmpty()) {
                        "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                    } else if (rawThumbnail.isNotEmpty()) {
                        rawThumbnail.replace("hqdefault.jpg", "maxresdefault.jpg")
                    } else {
                        ""
                    }

                    YoutubeVideo(
                        id = videoId,
                        title = item.name ?: "Untitled",
                        thumbnailUrl = bestThumbnail,
                        uploaderName = item.uploaderName ?: "",
                        duration = item.duration,
                        viewCount = item.viewCount,
                        uploadDate = formattedDate,
                        videoUrl = url
                    )
                }
        }
    }

    override fun getVideoStreamUrl(videoUrl: String): Flow<Result<String>> = flow {
        try {
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler)
            extractor.fetchPage()

            val streams = extractor.videoStreams
            val streamUrl = streams
                .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
                .maxByOrNull { it.height }?.content
                ?: streams.firstOrNull()?.content
                ?: throw Exception("Không tìm thấy luồng phát phù hợp")

            emit(Result.success(streamUrl))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getVideoDetail(videoUrl: String): Flow<Result<YoutubeVideoDetail>> = flow {
        try {
            val service = ServiceList.YouTube
            // Thêm tham số hl=vi-VN và gl=VN để gợi ý YouTube trả về tiếng Việt
            val localizedUrl = if (videoUrl.contains("?")) "$videoUrl&hl=vi-VN&gl=VN" else "$videoUrl?hl=vi-VN&gl=VN"
            val linkHandler = service.getStreamLHFactory().fromUrl(localizedUrl)
            val extractor = service.getStreamExtractor(linkHandler)
            extractor.fetchPage()

            val relatedItems = (extractor.relatedItems?.items ?: emptyList<InfoItem>())
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val url = item.url ?: ""
                    val videoId = url.substringAfter("v=", "").substringBefore("&")
                    val rawThumbnail = item.thumbnails.maxByOrNull { it.width * it.height }?.url
                        ?: item.thumbnails.lastOrNull()?.url
                        ?: ""
                    val bestThumbnail = if (videoId.isNotEmpty()) {
                        "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                    } else if (rawThumbnail.isNotEmpty()) {
                        rawThumbnail.replace("hqdefault.jpg", "maxresdefault.jpg")
                    } else ""

                    YoutubeVideo(
                        id = videoId,
                        title = item.name ?: "Untitled",
                        thumbnailUrl = bestThumbnail,
                        uploaderName = item.uploaderName ?: "",
                        duration = item.duration,
                        viewCount = item.viewCount,
                        uploadDate = "",
                        videoUrl = url
                    )
                }

            val videoId = videoUrl.substringAfter("v=", "").substringBefore("&")
            val avatars = extractor.uploaderAvatars
            val avatarUrl = if (!avatars.isNullOrEmpty()) {
                avatars.maxByOrNull { it.width * it.height }?.url ?: avatars.first().url
            } else ""

            val detail = YoutubeVideoDetail(
                id = videoId,
                title = extractor.name ?: "",
                description = extractor.description?.content ?: "",
                uploadDateTimestamp = extractor.uploadDate?.instant?.toEpochMilli() ?: 0L,
                viewCount = extractor.viewCount,
                videoUrl = videoUrl,
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                uploaderName = extractor.uploaderName ?: "",
                uploaderUrl = extractor.uploaderUrl ?: "",
                uploaderAvatarUrl = avatarUrl,
                relatedVideos = relatedItems
            )
            emit(Result.success(detail))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
