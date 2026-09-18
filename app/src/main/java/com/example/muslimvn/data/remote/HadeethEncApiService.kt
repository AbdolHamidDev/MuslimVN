package com.example.muslimvn.data.remote

import com.example.muslimvn.data.remote.model.HadeethDetailDto
import com.example.muslimvn.data.remote.model.HadeethListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface HadeethEncApiService {
    @GET("api/v1/hadeeths/list/")
    suspend fun getHadeeths(
        @Query("language") language: String = "vi",
        @Query("category_id") categoryId: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): HadeethListResponseDto

    @GET("api/v1/hadeeths/one/")
    suspend fun getHadeeth(
        @Query("id") id: String,
        @Query("language") language: String = "vi"
    ): HadeethDetailDto
}
