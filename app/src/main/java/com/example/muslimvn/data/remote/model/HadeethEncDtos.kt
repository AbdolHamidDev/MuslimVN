package com.example.muslimvn.data.remote.model

import com.google.gson.annotations.SerializedName

data class HadeethListResponseDto(
    val data: List<HadeethListItemDto> = emptyList(),
    val meta: HadeethMetaDto? = null
)

data class HadeethListItemDto(val id: String, val title: String? = null)

data class HadeethMetaDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null
)

data class HadeethDetailDto(
    val id: String,
    val title: String? = null,
    @SerializedName("hadeeth") val text: String? = null,
    val attribution: String? = null,
    val grade: String? = null,
    val explanation: String? = null,
    val categories: List<String> = emptyList()
)
