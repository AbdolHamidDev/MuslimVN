package com.example.muslimvn.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface QuranApiService {
    @GET("verses/by_key/{verseKey}")
    suspend fun getVerseWithAudio(
        @Path("verseKey") verseKey: String,
        @Query("words") words: Boolean = true,
        @Query("audio") recitationId: Int
    ): VerseAudioResponse
}

data class VerseWithAudio(
    @SerializedName("verse_key") val verseKey: String,
    val audio: AudioData
)

data class AudioData(
    val url: String,
    val segments: List<List<Double>> // [word_index, ?, start_ms, end_ms]
)

data class VerseAudioResponse(
    val verse: VerseWithAudio
)
