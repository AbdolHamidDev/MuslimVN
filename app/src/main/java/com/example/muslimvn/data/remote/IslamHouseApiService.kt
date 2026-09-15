package com.example.muslimvn.data.remote

import com.example.muslimvn.data.remote.model.IslamHouseResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface IslamHouseApiService {

    @GET("v3/paV29H2gm56kVLPy/main/get-author-items/{authorId}/showall/{displayLang}/{interfaceLang}/{page}/{limit}/json")
    suspend fun getAuthorItems(
        @Path("authorId") authorId: Long = 193689,
        @Path("displayLang") displayLang: String = "vi",
        @Path("interfaceLang") interfaceLang: String = "vi",
        @Path("page") page: Int,
        @Path("limit") limit: Int = 20
    ): IslamHouseResponse
}
