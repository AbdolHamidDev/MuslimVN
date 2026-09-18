package com.example.muslimvn.data.util

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetch + parse RSS podcast feed bằng OkHttp và XmlPullParser (streaming, không DOM).
 * Hỗ trợ trích xuất ảnh từ nhiều nguồn phổ biến: itunes:image, media:content, image tag
 * và bóc tách thông tin Channel Metadata.
 */
@Singleton
class RssParserUtility @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    data class RssEpisode(
        val id: String,
        val title: String,
        val description: String,
        val audioUrl: String,
        val artworkUrl: String?,
        val durationMs: Long,
        val pubDateMs: Long
    )

    data class RssChannelMetadata(
        val title: String,
        val description: String,
        val imageUrl: String?,
        val author: String?
    )

    private enum class TextTarget { TITLE, DESCRIPTION, DURATION, PUB_DATE, IMAGE_URL }

    private class ParsedItem {
        var title = ""
        var description = ""
        var audioUrl = ""
        var artworkUrl: String? = null
        var durationRaw = ""
        var pubDateRaw = ""
    }

    suspend fun fetchEpisodes(rssUrl: String): Result<List<RssEpisode>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(rssUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} khi tải feed $rssUrl")
                    }
                    val body = response.body ?: throw IOException("Feed rỗng: $rssUrl")
                    parse(body.byteStream())
                }
            }.onFailure { error ->
                Log.e(TAG, "Lỗi fetch/parse RSS: $rssUrl", error)
            }
        }

    suspend fun fetchChannelMetadata(rssUrl: String): Result<RssChannelMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(rssUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} khi tải feed $rssUrl")
                    }
                    val body = response.body ?: throw IOException("Feed rỗng: $rssUrl")
                    parseChannelMetadata(body.byteStream())
                }
            }.onFailure { error ->
                Log.e(TAG, "Lỗi fetch channel metadata: $rssUrl", error)
            }
        }

    private fun parseChannelMetadata(inputStream: InputStream): RssChannelMetadata {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(inputStream, null)
        }

        var channelTitle = ""
        var channelDescription = ""
        var channelImageUrl: String? = null
        var channelAuthor: String? = null
        val textBuffer = StringBuilder()
        var isInChannelImage = false
        var isInItem = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase(Locale.US)
            when (event) {
                XmlPullParser.START_TAG -> when (tagName) {
                    "item" -> isInItem = true
                    "title" -> if (!isInItem) textBuffer.setLength(0)
                    "description" -> if (!isInItem) textBuffer.setLength(0)
                    "author" -> if (!isInItem) textBuffer.setLength(0)
                    "image" -> {
                        if (!isInItem) {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) channelImageUrl = href
                            else isInChannelImage = true
                        }
                    }
                    "url" -> if (isInChannelImage && !isInItem) textBuffer.setLength(0)
                }
                XmlPullParser.TEXT -> if (!isInItem) textBuffer.append(parser.text)
                XmlPullParser.END_TAG -> when (tagName) {
                    "title" -> if (!isInItem && channelTitle.isEmpty()) channelTitle = textBuffer.toString().trim()
                    "description" -> if (!isInItem && channelDescription.isEmpty()) channelDescription = textBuffer.toString().trim()
                    "author" -> if (!isInItem && channelAuthor == null) channelAuthor = textBuffer.toString().trim()
                    "image" -> if (!isInItem) isInChannelImage = false
                    "url" -> if (isInChannelImage && !isInItem && channelImageUrl == null) {
                        channelImageUrl = textBuffer.toString().trim()
                    }
                }
            }
            if (isInItem) break
            event = parser.next()
        }
        return RssChannelMetadata(
            title = channelTitle,
            description = channelDescription,
            imageUrl = channelImageUrl,
            author = channelAuthor
        )
    }

    private fun parse(inputStream: InputStream): List<RssEpisode> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(inputStream, null)
        }

        val episodes = mutableListOf<RssEpisode>()
        var channelImageUrl: String? = null
        var currentItem: ParsedItem? = null
        var textTarget: TextTarget? = null
        val textBuffer = StringBuilder()
        var isInChannelImage = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase(Locale.US)
            when (event) {
                XmlPullParser.START_TAG -> when (tagName) {
                    "item" -> currentItem = ParsedItem()
                    "title" -> textTarget = beginText(currentItem, TextTarget.TITLE, textBuffer)
                    "description" -> textTarget = beginText(currentItem, TextTarget.DESCRIPTION, textBuffer)
                    "duration" -> textTarget = beginText(currentItem, TextTarget.DURATION, textBuffer)
                    "pubdate" -> textTarget = beginText(currentItem, TextTarget.PUB_DATE, textBuffer)
                    "image" -> {
                        val href = parser.getAttributeValue(null, "href")
                        if (href != null) {
                            if (currentItem != null) currentItem.artworkUrl = href
                            else channelImageUrl = href
                        } else if (currentItem == null) {
                            isInChannelImage = true
                        }
                    }
                    "url" -> {
                        if (isInChannelImage) {
                            textTarget = TextTarget.IMAGE_URL
                            textBuffer.setLength(0)
                        }
                    }
                    "content", "thumbnail" -> {
                        val url = parser.getAttributeValue(null, "url")
                        val type = parser.getAttributeValue(null, "type")
                        if (url != null && currentItem != null) {
                            if (tagName == "thumbnail" || type?.contains("image") == true) {
                                if (currentItem.artworkUrl == null) currentItem.artworkUrl = url
                            }
                        }
                    }
                    "enclosure" -> currentItem?.let { item ->
                        if (item.audioUrl.isEmpty() && isAudioEnclosure(parser)) {
                            item.audioUrl = parser.getAttributeValue(null, "url").orEmpty().trim()
                        }
                    }
                }
                XmlPullParser.TEXT -> if (textTarget != null) {
                    textBuffer.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (tagName) {
                    "item" -> {
                        if (currentItem?.artworkUrl == null) currentItem?.artworkUrl = channelImageUrl
                        currentItem?.toRssEpisode()?.let(episodes::add)
                        currentItem = null
                        textTarget = null
                    }
                    "image" -> isInChannelImage = false
                    "url" -> {
                        if (isInChannelImage && textTarget == TextTarget.IMAGE_URL) {
                            channelImageUrl = textBuffer.toString().trim()
                            textTarget = null
                        }
                    }
                    else -> {
                        if (textTarget != null && currentItem != null) {
                            commitText(currentItem, textTarget, textBuffer)
                            textTarget = null
                        }
                    }
                }
            }
            event = parser.next()
        }
        return episodes
    }

    private fun beginText(item: ParsedItem?, target: TextTarget, buffer: StringBuilder): TextTarget? {
        buffer.setLength(0)
        return if (item != null || target == TextTarget.IMAGE_URL || target == TextTarget.TITLE) target else null
    }

    private fun commitText(item: ParsedItem?, target: TextTarget?, buffer: StringBuilder) {
        if (item == null || target == null) return
        val value = buffer.toString().trim()
        if (value.isEmpty()) return
        when (target) {
            TextTarget.TITLE -> item.title = value
            TextTarget.DESCRIPTION -> if (item.description.isEmpty()) item.description = value
            TextTarget.DURATION -> item.durationRaw = value
            TextTarget.PUB_DATE -> item.pubDateRaw = value
            else -> {}
        }
    }

    private fun isAudioEnclosure(parser: XmlPullParser): Boolean {
        val type = parser.getAttributeValue(null, "type").orEmpty().lowercase(Locale.US)
        val url = parser.getAttributeValue(null, "url").orEmpty().lowercase(Locale.US)
        return type.contains("audio") || url.endsWith(".mp3") || url.endsWith(".m4a") || url.endsWith(".aac")
    }

    private fun ParsedItem.toRssEpisode(): RssEpisode? {
        if (title.isEmpty() && audioUrl.isEmpty()) return null
        return RssEpisode(
            id = stableId(audioUrl.ifEmpty { title }),
            title = title,
            description = description,
            audioUrl = audioUrl,
            artworkUrl = artworkUrl,
            durationMs = parseDurationToMs(durationRaw),
            pubDateMs = parsePubDateToMs(pubDateRaw)
        )
    }

    companion object {
        private const val TAG = "RssParserUtility"
        private const val USER_AGENT = "MuslimVN/1.0 (Android; Podcast)"

        fun stableId(source: String): String =
            MessageDigest.getInstance("MD5")
                .digest(source.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        fun parseDurationToMs(raw: String): Long {
            val parts = raw.trim().split(':').map { it.trim().toLongOrNull() ?: return 0L }
            var multiplier = 1000L
            var totalMs = 0L
            for (part in parts.reversed()) {
                totalMs += part * multiplier
                multiplier *= 60
            }
            return totalMs
        }

        private val PUB_DATE_PATTERNS = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )

        fun parsePubDateToMs(raw: String): Long {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return 0L
            for (pattern in PUB_DATE_PATTERNS) {
                try {
                    val format = SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = true
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    format.parse(trimmed)?.time?.let { return it }
                } catch (_: Exception) { }
            }
            return 0L
        }
    }
}
