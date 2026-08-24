package com.example.muslimvn.data.remote

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Aladhan prayer-times & Hijri calendar API.
 *
 * `gregorianToHijriCalendar/{month}/{year}` returns the full Hijri calendar for a
 * given Gregorian month (e.g. `/1/2024` for January 2024).
 *
 * The payload is intentionally kept as a raw [JsonObject] and parsed defensively
 * by [AladhanCalendarParser], because the public endpoint occasionally serves either
 * the standard nested shape (`hijri`/`gregorian` objects) or a compact flat shape.
 */
interface AladhanApiService {

    @GET("v1/gregorianToHijriCalendar/{month}/{year}")
    suspend fun getGregorianToHijriCalendar(
        @Path("month") month: Int,
        @Path("year") year: Int
    ): JsonObject

    @GET("v1/gToH/{date}")
    suspend fun getGregorianToHijri(
        @Path("date") date: String // "dd-MM-yyyy"
    ): JsonObject
}