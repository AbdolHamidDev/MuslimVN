package com.example.muslimvn.data.local.masjid

import com.example.muslimvn.domain.models.masjid.MasjidDatabase
import com.example.muslimvn.domain.models.masjid.MasjidDatabaseValidator
import com.example.muslimvn.domain.models.masjid.MasjidType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MasjidJsonDataSourceTest {

    private val dataSource = MasjidJsonDataSource(
        assetReader = MasjidAssetReader { error("Asset reader is not used in parse tests") }
    )

    @Test
    fun jsonParsing_returnsDatabase() {
        val database = dataSource.parse(validDatabaseJson())

        assertNotNull(database)
        assertEquals(1, database?.schemaVersion)
        assertEquals("ho-chi-minh", database?.province?.id)
    }

    @Test
    fun validMasjidParsing_mapsFields() {
        val masjid = dataSource.parse(validDatabaseJson())?.masjids?.first()

        assertNotNull(masjid)
        assertEquals("hcm-001", masjid?.id)
        assertEquals("Masjid Test", masjid?.name)
        assertEquals(MasjidType.MASJID, masjid?.type)
    }

    @Test
    fun nullableFields_remainNullWhenUnknown() {
        val masjid = dataSource.parse(validDatabaseJson())?.masjids?.first()

        assertNull(masjid?.location?.latitude)
        assertNull(masjid?.location?.longitude)
        assertNull(masjid?.facilities?.womenPrayerArea)
        assertNull(masjid?.contact)
    }

    @Test
    fun multipleJummahTimes_areParsed() {
        val masjid = dataSource.parse(validDatabaseJson(jummah = listOf("12:15", "13:15")))
            ?.masjids
            ?.first()

        assertEquals(listOf("12:15", "13:15"), masjid?.prayer?.jummah)
    }

    @Test
    fun masjidType_preservesTieuThangDuong() {
        val masjid = dataSource.parse(validDatabaseJson(type = "TIEU_THANG_DUONG"))
            ?.masjids
            ?.first()

        assertEquals(MasjidType.TIEU_THANG_DUONG, masjid?.type)
    }

    @Test
    fun mediaVideos_areParsedWhenPresent() {
        val json = validDatabaseJson().replace(
          "\"images\": []",
          "\"images\": [\"https://example.com/img.jpg\"], \"videos\": [\"https://example.com/vid.mp4\"]"
        )
        val masjid = dataSource.parse(json)?.masjids?.first()

        assertEquals(listOf("https://example.com/img.jpg"), masjid?.media?.images)
        assertEquals(listOf("https://example.com/vid.mp4"), masjid?.media?.videos)
    }

    @Test
    fun invalidCoordinates_areValidationErrors() {
        val database = decodeWithoutValidation(
            validDatabaseJson(latitude = 91.0, longitude = 181.0)
        )

        val result = MasjidDatabaseValidator.validate(database)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Invalid latitude") })
        assertTrue(result.errors.any { it.message.contains("Invalid longitude") })
    }

    @Test
    fun duplicateIds_areValidationErrors() {
        val first = masjidJson(id = "hcm-001")
        val second = masjidJson(id = "hcm-001")
        val database = decodeWithoutValidation(validDatabaseJson(masjids = listOf(first, second)))

        val result = MasjidDatabaseValidator.validate(database)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Duplicate masjid id: hcm-001") })
    }

    @Test
    fun invalidJummahTimeFormat_isValidationError() {
        val database = decodeWithoutValidation(validDatabaseJson(jummah = listOf("25:99")))

        val result = MasjidDatabaseValidator.validate(database)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Invalid Jumu'ah time") })
    }

    @Test
    fun missingOrganizationReference_isValidationError() {
        val database = decodeWithoutValidation(validDatabaseJson(organizationId = "missing-org"))

        val result = MasjidDatabaseValidator.validate(database)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Missing organization reference") })
    }

    @Test
    fun malformedJson_returnsNull() {
        assertNull(dataSource.parse("{ malformed json"))
    }

    @Test
    fun loadingHoChiMinhAsset_returnsSampleDataset() = runBlocking {
        val assetRoot = File("src/main/assets")
        val assetDataSource = MasjidJsonDataSource(
            assetReader = MasjidAssetReader { path ->
                File(assetRoot, path).readText()
            }
        )

        val database = assetDataSource.loadProvince("ho-chi-minh")

        assertNotNull(database)
        assertEquals("ho-chi-minh", database?.province?.id)
        assertEquals(16, database?.masjids?.size)
        assertTrue(database?.masjids.orEmpty().any { it.name == "Masjid al-Rahim" })
        assertTrue(database?.managementOrganizations.orEmpty().isNotEmpty())
    }

    private fun decodeWithoutValidation(jsonText: String): MasjidDatabase =
        Json.decodeFromString<MasjidDatabase>(jsonText)

    private fun validDatabaseJson(
        type: String = "MASJID",
        latitude: Double? = null,
        longitude: Double? = null,
        jummah: List<String> = emptyList(),
        organizationId: String? = "hcm-org",
        masjids: List<String> = listOf(
            masjidJson(
                type = type,
                latitude = latitude,
                longitude = longitude,
                jummah = jummah,
                organizationId = organizationId
            )
        )
    ): String = """
        {
          "schemaVersion": 1,
          "province": {
            "id": "ho-chi-minh",
            "name": "Ho Chi Minh City",
            "countryCode": "VN"
          },
          "managementOrganizations": [
            {
              "id": "hcm-org",
              "name": "Representative Board",
              "type": "PROVINCIAL_ISLAMIC_REPRESENTATIVE_BOARD"
            }
          ],
          "masjids": [
            ${masjids.joinToString(",")}
          ]
        }
    """.trimIndent()

    private fun masjidJson(
        id: String = "hcm-001",
        type: String = "MASJID",
        latitude: Double? = null,
        longitude: Double? = null,
        jummah: List<String> = emptyList(),
        organizationId: String? = "hcm-org"
    ): String {
        val latitudeJson = latitude?.toString() ?: "null"
        val longitudeJson = longitude?.toString() ?: "null"
        val jummahJson = jummah.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val organizationIdJson = organizationId?.let { "\"$it\"" } ?: "null"

        return """
            {
              "id": "$id",
              "name": "Masjid Test",
              "nativeName": null,
              "aliases": [],
              "type": "$type",
              "location": {
                "address": null,
                "province": "Ho Chi Minh City",
                "district": null,
                "commune": null,
                "latitude": $latitudeJson,
                "longitude": $longitudeJson
              },
              "contact": null,
              "prayer": {
                "jummah": $jummahJson,
                "eidPrayer": []
              },
              "facilities": {
                "womenPrayerArea": null,
                "wudu": null,
                "parking": null,
                "quran": null,
                "quranClass": null
              },
              "services": null,
              "management": {
                "organizationId": $organizationIdJson,
                "boardName": null
              },
              "media": {
                "images": []
              },
              "verification": {
                "status": "UNVERIFIED",
                "sources": [],
                "lastVerifiedAt": null
              },
              "notes": null
            }
        """.trimIndent()
    }
}
