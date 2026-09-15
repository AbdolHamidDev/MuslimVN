package com.example.muslimvn.data.remote.model

import com.google.gson.annotations.SerializedName

data class IslamHouseResponse(
    @SerializedName("data") val data: List<IslamHouseItem>?,
    @SerializedName("links") val links: PaginationLinks?
)

data class IslamHouseItem(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("type") val type: String?, // "books", "audios", "articles", "fatwas", etc.
    @SerializedName("add_date") val addDate: Long?,
    @SerializedName("description") val description: String?,
    @SerializedName("source_language") val sourceLanguage: String?,
    @SerializedName("translated_language") val translatedLanguage: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("api_url") val apiUrl: String?,
    @SerializedName("prepared_by") val preparedBy: List<AuthorInfo>?,
    @SerializedName("attachments") val attachments: List<Attachment>?
)

data class AuthorInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("kind") val kind: String?
)

data class Attachment(
    @SerializedName("order") val order: Int?,
    @SerializedName("size") val size: String?,
    @SerializedName("extension_type") val extensionType: String?, // "PDF", "MP3", "ZIP", etc.
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?
)

data class PaginationLinks(
    @SerializedName("next") val next: String?,
    @SerializedName("prev") val prev: String?,
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("pages_number") val pagesNumber: Int?,
    @SerializedName("total_items") val totalItems: Int?
)
