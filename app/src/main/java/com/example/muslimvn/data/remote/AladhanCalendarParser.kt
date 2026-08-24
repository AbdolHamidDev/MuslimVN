package com.example.muslimvn.data.remote

import com.example.muslimvn.data.local.entities.HijriDayEntity
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust mapper from the Aladhan monthly calendar payload into [HijriDayEntity] rows.
 *
 * The endpoint may serve two different shapes depending on version/server state:
 *  - standard: `data.hijriDays` is an object keyed "1".."31", each day containing nested
 *    `hijri` / `gregorian` objects (mirroring the single-date `gToH` format);
 *  - compact:  `data.hijriDays` days carry flat `gregorianDate`, `hijriDay`, `hijriMonth`,
 *    `hijriYear`, `weekday` fields.
 *
 * The parser probes both shapes and silently skips malformed rows. In the worst case it
 * returns an empty list and the repository falls back to the local `HijrahChronology`
 * calculation, so the app can never crash.
 */
@Singleton
class AladhanCalendarParser @Inject constructor() {

    fun parseMonthlyCalendar(body: JsonObject, month: Int, year: Int): List<HijriDayEntity> {
        val data = body.asJsonObject("data") ?: return emptyList()
        val container = data.asJsonObject("hijriDays") ?: data
        return extractFromContainer(container, month, year)
    }

    private fun extractFromContainer(
        container: JsonObject?,
        month: Int,
        year: Int
    ): List<HijriDayEntity> {
        if (container == null) return emptyList()
        return container
            .entrySet()
            .mapNotNull { (_, value) ->
                val dayObj = value.asJsonObjectOrNull() ?: return@mapNotNull null
                parseDay(dayObj, month, year)
            }
            .sortedBy { it.gregorianDate }
    }
private fun parseDay(day: JsonObject, month: Int, year: Int): HijriDayEntity? {
        val gregorianDate: LocalDate
        val hijriDay: Int
        val hijriMonth: Int
        val hijriYear: Int
        val eventsJson: String

        // ---- Shape 1: nested `gregorian` / `hijri` objects -------------------------
        val gregorian = day.asJsonObject("gregorian")
        val hijri = day.asJsonObject("hijri")
        if (gregorian != null && hijri != null) {
            gregorianDate = parseDate(gregorian.stringValue("date")) ?: return null
            hijriDay = hijri.stringValue("day")?.toIntOrNull() ?: return null
            hijriMonth = hijri.asJsonObject("month")?.stringValue("number")?.toIntOrNull()
                ?: day.stringValue("hijriMonth")?.toIntOrNull()
                ?: return null
            hijriYear = hijri.stringValue("year")?.toIntOrNull() ?: return null
            eventsJson = formatHolidays(hijri.asJsonArray("holidays"))
        } else {
            // ---- Shape 2: flat fields ------------------------------------------------
            gregorianDate = parseDate(
                day.stringValue("gregorianDate") ?: day.stringValue("date")
            ) ?: return null
            hijriDay = day.stringValue("hijriDay")?.toIntOrNull() ?: return null
            hijriMonth = day.stringValue("hijriMonth")?.toIntOrNull() ?: return null
            hijriYear = day.stringValue("hijriYear")?.toIntOrNull() ?: return null
            eventsJson = formatHolidays(
                day.asJsonArray("hijri_holidays") ?: day.asJsonArray("events")
            )
        }

        // Only keep rows that actually belong to the requested Gregorian month/year.
        if (gregorianDate.monthValue != month || gregorianDate.year != year) return null

        return HijriDayEntity(
            gregorianDate = gregorianDate.toString(),
            hijriDay = hijriDay,
            hijriMonth = hijriMonth,
            hijriYear = hijriYear,
            dayOfWeek = gregorianDate.dayOfWeek.value,
            eventsJson = eventsJson,
            gregorianMonth = month,
            gregorianYear = year,
            cachedAtEpochMillis = System.currentTimeMillis()
        )
    }

    // ---- helpers ------------------------------------------------------------------

    private fun formatHolidays(holidays: JsonArray?): String {
        if (holidays == null) return "[]"
        return buildString {
            append('[')
            var first = true
            for (element in holidays) {
                val name = element.asJsonObjectOrNull()?.stringValue("name")
                    ?: element.takeIf { it.isJsonPrimitive }?.asString
                    ?: continue
                if (!first) append(',')
                first = false
                append('"').append(name.replace("\"", "\\\"")).append('"')
            }
            append(']')
        }
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        for (pattern in DATE_PATTERNS) {
            runCatching { return LocalDate.parse(trimmed, pattern) }
        }
        return null
    }

    private fun JsonObject.asJsonObject(field: String): JsonObject? =
        get(field)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.stringValue(field: String): String? =
        get(field)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.asJsonArray(field: String): JsonArray? =
        get(field)?.takeIf { it.isJsonArray }?.asJsonArray

    companion object {
        private val DATE_PATTERNS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
        )
    }
}