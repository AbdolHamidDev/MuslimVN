package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Một học giả Islamic trong tính năng Podcast.
 *
 * @param id Mã định danh ổn định, ví dụ "mufti-menk" (khớp với scholars.json).
 * @param avatarPath Đường dẫn tương đối trong assets, ví dụ "images/podcast/mufti-menk.webp".
 * @param rssUrl URL feed RSS nguồn các tập phát thanh/podcast.
 * @param tags Danh sách mã phân loại (tafsir, fiqh, aqidah…) — lưu dạng CSV qua TypeConverter.
 * @param featured Có nằm trong mục "Học giả nổi bật" trên trang chủ Podcast hay không.
 */
@Entity(tableName = "scholars")
data class ScholarEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val bio: String,
    val avatarPath: String,
    val rssUrl: String,
    val tags: List<String>,
    val featured: Boolean
)
