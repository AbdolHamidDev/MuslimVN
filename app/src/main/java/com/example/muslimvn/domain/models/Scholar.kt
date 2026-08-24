package com.example.muslimvn.domain.models

/**
 * Học giả Islamic trong tính năng Podcast (model tầng domain).
 *
 * @param tags Mã phân loại nội dung, khớp với id category trong scholars.json.
 * @param featured Có hiển thị trong carousel "Học giả nổi bật" hay không.
 */
data class Scholar(
    val id: String,
    val name: String,
    val title: String,
    val bio: String,
    val avatarPath: String,
    val rssUrl: String,
    val tags: List<String>,
    val featured: Boolean
)
