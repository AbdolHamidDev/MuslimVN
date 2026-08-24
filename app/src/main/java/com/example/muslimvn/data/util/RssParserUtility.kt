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
 *
 * Chỉ quan tâm các thẻ bên trong <item>:
 *  - <title>, <description>
 *  - <enclosure url="..." type="audio/...">  -> link MP3
 *  - <itunes:duration>                          -> "3600" giây | "MM:SS" | "HH:MM:SS"
 *  - <pubDate>                                  -> RFC-822, ví dụ "Wed, 21 Aug 2024 09:00:00 +0000"
 *
 * Mọi lỗi mạng/parse được bọc trong Result để caller (repository) xử lý offline-first.
 */
@Singleton
class RssParserUtility @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    /** Kết quả parse tối thiểu cho một tập phát. */
    data class RssEpisode(
        val id: String,
        val title: String,
        val description: String,
        val audioUrl: String,
        val durationMs: Long,
        val pubDateMs: Long
    )

    private enum class TextTarget { TITLE, DESCRIPTION, DURATION, PUB_DATE }

    /** Bộ đệm tạm cho item đang parse. */
    private class ParsedItem {
        var title = ""
        var description = ""
        var audioUrl = ""
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

    // ── Parse XML stream ────────────────────────────────────────────────────────

    private fun parse(inputStream: InputStream): List<RssEpisode> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(inputStream, null)
        }

        val episodes = mutableListOf<RssEpisode>()
        var currentItem: ParsedItem? = null
        var textTarget: TextTarget? = null
        val textBuffer = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.US)) {
                    "item" -> currentItem = ParsedItem()
                    "title" -> beginText(currentItem, TextTarget.TITLE, textBuffer).let { textTarget = it }
                    "description" -> beginText(currentItem, TextTarget.DESCRIPTION, textBuffer).let { textTarget = it }
                    "duration" -> beginText(currentItem, TextTarget.DURATION, textBuffer).let { textTarget = it }
                    "pubdate" -> beginText(currentItem, TextTarget.PUB_DATE, textBuffer).let { textTarget = it }
                    "enclosure" -> currentItem?.let { item ->
                        if (item.audioUrl.isEmpty() && isAudioEnclosure(parser)) {
                            item.audioUrl = parser.getAttributeValue(null, "url").orEmpty().trim()
                        }
                    }
                }

                XmlPullParser.TEXT -> if (textTarget != null && currentItem != null) {
                    textBuffer.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (parser.name.lowercase(Locale.US)) {
                    "item" -> {
                        currentItem?.toRssEpisode()?.let(episodes::add)
                        currentItem = null
                        textTarget = null
                        textBuffer.setLength(0)
                    }
                    else -> {
                        // Đóng thẻ trường văn bản -> commit nội dung đã gom vào item.
                        commitText(currentItem, textTarget, textBuffer)
                        textTarget = null
                        textBuffer.setLength(0)
                    }
                }
            }
            event = parser.next()
        }
        return episodes
    }

    /** Bắt đầu gom text nếu đang ở trong <item>; trả về target đang hiệu lực (hoặc null). */
    private fun beginText(
        item: ParsedItem?,
        target: TextTarget,
        buffer: StringBuilder
    ): TextTarget? {
        buffer.setLength(0)
        return if (item != null) target else null
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
        }
    }

    private fun isAudioEnclosure(parser: XmlPullParser): Boolean {
        val type = parser.getAttributeValue(null, "type").orEmpty().lowercase(Locale.US)
        val url = parser.getAttributeValue(null, "url").orEmpty().lowercase(Locale.US)
        return type.contains("audio") || url.endsWith(".mp3") || url.endsWith(".m4a") || url.endsWith(".aac")
    }

    private fun ParsedItem.toRssEpisode(): RssEpisode? {
        if (title.isEmpty() && audioUrl.isEmpty()) return null // item lỗi/rác
        return RssEpisode(
            id = stableId(audioUrl.ifEmpty { title }),
            title = title,
            description = description,
            audioUrl = audioUrl,
            durationMs = parseDurationToMs(durationRaw),
            pubDateMs = parsePubDateToMs(pubDateRaw)
        )
    }

    companion object {
        private const val TAG = "RssParserUtility"
        private const val USER_AGENT = "MuslimVN/1.0 (Android; Podcast)"

        /** ID ổn định từ URL (MD5 hex) để các lần refresh map về đúng dòng trong Room. */
        fun stableId(source: String): String =
            MessageDigest.getInstance("MD5")
                .digest(source.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        /** "3600" (giây) | "MM:SS" | "HH:MM:SS" -> millisecond. Trả 0 nếu không hiểu. */
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

        /** RFC-822 / ISO-8601 -> epoch ms; trả 0 khi không parse được (không ném exception). */
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
                } catch (_: Exception) {
                    // Thử pattern kế tiếp.
                }
            }
            Log.w(TAG, "Không parse được pubDate: '$trimmed'")
            return 0L
        }
    }
}
