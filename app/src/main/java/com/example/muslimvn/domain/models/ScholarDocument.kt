package com.example.muslimvn.domain.models

data class ScholarDocument(
    val id: Long,
    val title: String,
    val type: String, // "books", "audios", "articles", etc.
    val description: String?,
    val addDate: Long?,
    val fileExtension: String?, // "PDF", "MP3", etc.
    val fileSize: String?,
    val downloadUrl: String?,
    val detailUrl: String?,
    val imageUrl: String? = null
)
