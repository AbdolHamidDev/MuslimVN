package com.example.muslimvn.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Quản lý màu sắc ngữ nghĩa riêng cho các danh mục nội dung (Podcast, Tài liệu, Chủ đề bài giảng).
 * Giúp tách biệt màu ngữ nghĩa nội dung khỏi UI Theme chính.
 */
object CategoryColors {

    /**
     * Trả về màu sắc đậm riêng biệt cho từng tag phân loại Podcast (Aqidah, Fiqh, Tafsir...)
     */
    fun getPodcastCategoryBadgeColor(tag: String): Color = when (tag.lowercase()) {
        "aqidah" -> Color(0xFF1E3A8A)       // Đậm Xanh Dương Navy
        "fiqh" -> Color(0xFF065F46)         // Đậm Xanh Lục Emerald
        "tafsir" -> Color(0xFF5B21B6)       // Đậm Tím Royal Purple
        "tazkiyah" -> Color(0xFF881337)      // Đậm Đỏ Rượu Crimson
        "contemporary" -> Color(0xFF78350F) // Đậm Nâu Amber
        "inspiration" -> Color(0xFF0F766E)  // Đậm Xanh Ngọc Teal
        else -> Color(0xFF334155)            // Đậm Xám Slate
    }

    /**
     * Trả về cặp màu nền thẻ (container) và màu icon cho loại file tài liệu (PDF, MP3, DOCX...).
     */
    data class FileExtensionColors(
        val containerColor: Color,
        val iconColor: Color
    )

    fun getDocumentFileColors(fileExt: String): FileExtensionColors = when (fileExt.uppercase()) {
        "PDF" -> FileExtensionColors(Color(0xFFFCE8E6), Color(0xFFC5221F))
        "MP3" -> FileExtensionColors(Color(0xFFE8F0FE), Color(0xFF1A73E8))
        "DOCX" -> FileExtensionColors(Color(0xFFEAF2F8), Color(0xFF2471A3))
        else -> FileExtensionColors(Color(0xFFE8F0FE), Color(0xFF055136))
    }

    /**
     * Trả về bộ màu chủ đề bài giảng (primary, secondary, title) theo ID.
     */
    data class TopicCoverColors(
        val primaryColor: Color,
        val secondaryColor: Color,
        val title: String
    )

    fun getScholarTopicColors(documentId: Long): TopicCoverColors {
        return when ((documentId % 4).toInt()) {
            0 -> TopicCoverColors(Color(0xFF1B5E20), Color(0xFFC8E6C9), "Islam")
            1 -> TopicCoverColors(Color(0xFF0D47A1), Color(0xFFBBDEFB), "Sunnah")
            2 -> TopicCoverColors(Color(0xFF4A148C), Color(0xFFE1BEE7), "Tawhid")
            else -> TopicCoverColors(Color(0xFFE65100), Color(0xFFFFE0B2), "Fiqh")
        }
    }
}
