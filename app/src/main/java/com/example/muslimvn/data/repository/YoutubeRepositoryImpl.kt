package com.example.muslimvn.data.repository

import android.net.Uri
import com.example.muslimvn.core.utils.TimeUtils
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.models.YoutubeVideoDetail
import com.example.muslimvn.domain.repository.StreamDownloadInfo
import com.example.muslimvn.domain.repository.VideoAndAudioStreamInfo
import com.example.muslimvn.domain.repository.YoutubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.collections.filterIsInstance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRepositoryImpl @Inject constructor() : YoutubeRepository {

    private val videoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<List<YoutubeVideo>, Long>>()
    private val channelVideoTabs = java.util.concurrent.ConcurrentHashMap<String, ListLinkHandler>()
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
            // Standard NewPipe flow: resolve the channel, then load its official
            // Videos tab. Locale is taken from NewPipe.init(), not URL parameters.
            val channelInfo = ChannelInfo.getInfo(service, channelUrl)
            val videosTab = channelInfo.tabs.firstOrNull { tab ->
                tab.contentFilters.firstOrNull() == ChannelTabs.VIDEOS
            } ?: throw IllegalStateException("Kênh không có tab video")
            val videosInfo = ChannelTabInfo.getInfo(service, videosTab)

            channelVideoTabs[channelUrl] = videosTab
            videosInfo.nextPage?.let { nextPages[channelUrl] = it }
                ?: nextPages.remove(channelUrl)

            val videos = videosInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toYoutubeVideo() }

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
            val videosTab = channelVideoTabs[channelUrl] ?: return@runCatching emptyList()
            val nextPage = nextPages[channelUrl] ?: return@runCatching emptyList()

            val page = ChannelTabInfo.getMoreItems(ServiceList.YouTube, videosTab, nextPage)
            if (page.nextPage != null) {
                nextPages[channelUrl] = page.nextPage!!
            } else {
                nextPages.remove(channelUrl)
            }

            page.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toYoutubeVideo() }
        }
    }

    override fun getVideoStreamUrl(videoUrl: String): Flow<Result<String>> = flow {
        try {
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val streams = streamInfo.videoStreams
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

    override fun getAudioStreamUrl(videoUrl: String): Flow<Result<String>> = flow {
        try {
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val audioStreams = streamInfo.audioStreams
            // YouTube có thể trả track DUBBED (giọng AI) cùng với track ORIGINAL. Chọn
            // ORIGINAL trước bitrate để tệp podcast luôn giữ tiếng/giọng của video gốc.
            val m4aAudio = selectOriginalM4aAudioStream(audioStreams)
                ?: throw Exception("Không tìm thấy luồng âm thanh M4A phù hợp")

            val audioUrl = m4aAudio.content
                ?: throw Exception("URL luồng âm thanh M4A trống")

            emit(Result.success(audioUrl))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getMediaStreamInfo(videoUrl: String): Flow<Result<VideoAndAudioStreamInfo>> = flow {
        try {
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val durationSeconds = streamInfo.duration

            val videoStreams = streamInfo.videoStreams
            val bestVideoStream = videoStreams
                .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
                .maxByOrNull { it.height }
                ?: videoStreams.firstOrNull()

            val videoInfo = if (bestVideoStream != null && !bestVideoStream.content.isNullOrEmpty()) {
                val size = if (bestVideoStream.bitrate > 0 && durationSeconds > 0) {
                    (bestVideoStream.bitrate.toLong() / 8L) * durationSeconds
                } else 0L
                StreamDownloadInfo(bestVideoStream.content, size)
            } else null

            val audioStreams = streamInfo.audioStreams
            // Dùng cùng lựa chọn ORIGINAL với getAudioStreamUrl() để dung lượng trong
            // dialog khớp track M4A thực tế sẽ được tải.
            val m4aAudioStream = selectOriginalM4aAudioStream(audioStreams)

            val selectedAudioContent = m4aAudioStream?.content
            val selectedAudioBitrate = m4aAudioStream?.averageBitrate ?: 0

            val audioInfo = if (!selectedAudioContent.isNullOrEmpty()) {
                val size = if (selectedAudioBitrate > 0 && durationSeconds > 0) {
                    (selectedAudioBitrate.toLong() / 8L) * durationSeconds
                } else 0L
                StreamDownloadInfo(selectedAudioContent, size)
            } else null

            emit(Result.success(VideoAndAudioStreamInfo(videoInfo, audioInfo)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    private fun selectOriginalM4aAudioStream(audioStreams: List<AudioStream>): AudioStream? {
        val m4aStreams = audioStreams.filter {
            it.format == MediaFormat.M4A || it.format?.name?.equals("M4A", ignoreCase = true) == true
        }
        return m4aStreams
            .filter { it.audioTrackType == AudioTrackType.ORIGINAL }
            .maxByOrNull { it.averageBitrate }
            // Old videos may not expose a track type. In that case accept the unspecified track,
            // but never prefer an explicitly dubbed or descriptive one.
            ?: m4aStreams
                .filter { it.audioTrackType != AudioTrackType.DUBBED && it.audioTrackType != AudioTrackType.DESCRIPTIVE }
                .maxByOrNull { it.averageBitrate }
            ?: m4aStreams.maxByOrNull { it.averageBitrate }
    }

    override fun getVideoDetail(videoUrl: String): Flow<Result<YoutubeVideoDetail>> = flow {
        try {
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val relatedItems = mutableListOf<YoutubeVideo>()
            for (item in streamInfo.relatedItems.filterIsInstance<StreamInfoItem>()) {
                relatedItems += item.toYoutubeVideo(includeUploadDate = false)
            }

            val videoId = streamInfo.id
            val avatars = streamInfo.uploaderAvatars
            val avatarUrl = if (avatars.isNotEmpty()) {
                avatars.maxByOrNull { it.width * it.height }?.url ?: avatars.first().url
            } else ""
            val thumbnailUrl = streamInfo.thumbnails
                .maxByOrNull { it.width * it.height }?.url.orEmpty()

            val detail = YoutubeVideoDetail(
                id = videoId,
                title = streamInfo.name.orEmpty(),
                description = streamInfo.description?.content.orEmpty(),
                uploadDateTimestamp = streamInfo.uploadDate?.instant?.toEpochMilli() ?: 0L,
                viewCount = streamInfo.viewCount,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                uploaderName = streamInfo.uploaderName.orEmpty(),
                uploaderUrl = streamInfo.uploaderUrl.orEmpty(),
                uploaderAvatarUrl = avatarUrl,
                relatedVideos = relatedItems
            )
            emit(Result.success(detail))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    private fun StreamInfoItem.toYoutubeVideo(includeUploadDate: Boolean = true): YoutubeVideo {
        val url = url.orEmpty()
        val timestamp = uploadDate?.instant?.toEpochMilli() ?: 0L
        val formattedDate = if (includeUploadDate && timestamp > 0) {
            TimeUtils.getRelativeTimeSpanString(timestamp)
        } else {
            ""
        }
        val videoId = videoIdFromUrl(url)
        val rawThumbnail = thumbnails.maxByOrNull { it.width * it.height }?.url
            ?: thumbnails.lastOrNull()?.url.orEmpty()
        return YoutubeVideo(
            id = videoId,
            title = name ?: "Untitled",
            // Use exactly the thumbnail URL selected by NewPipe. A constructed
            // maxres URL is not guaranteed to exist for every YouTube video.
            thumbnailUrl = rawThumbnail,
            uploaderName = uploaderName.orEmpty(),
            duration = duration,
            viewCount = viewCount,
            uploadDate = formattedDate,
            videoUrl = url
        )
    }

    private fun videoIdFromUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.getQueryParameter("v")
            ?: uri.lastPathSegment.orEmpty()
    }

}
