package com.example.muslimvn.data.util

import android.util.Xml
import com.example.muslimvn.core.utils.TimeUtils
import com.example.muslimvn.domain.models.YoutubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRssParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun fetchVideos(channelId: String): Result<List<YoutubeVideo>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId&hl=vi-VN"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept-Language", "vi-VN,vi;q=0.9")
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body?.byteStream() ?: throw Exception("Empty body")
                parse(body)
            }
        }
    }

    private fun parse(inputStream: InputStream): List<YoutubeVideo> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(inputStream, null)
        }

        val videos = mutableListOf<YoutubeVideo>()
        var currentVideo: YoutubeVideoBuilder? = null
        var textTarget: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "entry" -> currentVideo = YoutubeVideoBuilder()
                        "videoId" -> textTarget = "videoId"
                        "title" -> if (currentVideo != null) textTarget = "title"
                        "published" -> textTarget = "published"
                        "name" -> textTarget = "author"
                        "thumbnail" -> {
                            currentVideo?.thumbnailUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                        "link" -> {
                            if (parser.getAttributeValue(null, "rel") == "alternate") {
                                currentVideo?.videoUrl = parser.getAttributeValue(null, "href") ?: ""
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        when (textTarget) {
                            "videoId" -> currentVideo?.id = text
                            "title" -> currentVideo?.title = text
                            "published" -> currentVideo?.uploadDate = text
                            "author" -> currentVideo?.uploaderName = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "entry") {
                        currentVideo?.build()?.let { videos.add(it) }
                        currentVideo = null
                    }
                    textTarget = null
                }
            }
            event = parser.next()
        }
        return videos
    }

    private class YoutubeVideoBuilder {
        var id: String = ""
        var title: String = ""
        var thumbnailUrl: String = ""
        var uploaderName: String = ""
        var uploadDate: String = ""
        var videoUrl: String = ""

        fun build(): YoutubeVideo {
            val highResThumbnail = if (id.isNotEmpty()) {
                "https://img.youtube.com/vi/$id/maxresdefault.jpg"
            } else if (thumbnailUrl.isNotEmpty()) {
                thumbnailUrl.replace("hqdefault.jpg", "maxresdefault.jpg")
                    .replace("mqdefault.jpg", "maxresdefault.jpg")
                    .replace("default.jpg", "maxresdefault.jpg")
                    .replace("sddefault.jpg", "maxresdefault.jpg")
            } else {
                ""
            }

            val relativeDate = try {
                // RSS format is usually ISO 8601: 2023-10-27T10:00:00+00:00
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                val date = sdf.parse(uploadDate)
                if (date != null) TimeUtils.getRelativeTimeSpanString(date.time) else uploadDate
            } catch (e: Exception) {
                uploadDate
            }

            return YoutubeVideo(
                id = id,
                title = title,
                thumbnailUrl = highResThumbnail,
                uploaderName = uploaderName,
                duration = 0,
                viewCount = 0,
                uploadDate = relativeDate,
                videoUrl = videoUrl
            )
        }
    }
}
