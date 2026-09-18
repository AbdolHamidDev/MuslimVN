package com.example.muslimvn.data.repository

import com.example.muslimvn.data.local.masjid.MasjidAssetReader
import com.example.muslimvn.data.local.masjid.MasjidJsonDataSource
import com.example.muslimvn.domain.models.masjid.MasjidType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalMasjidRepositoryTest {

    @Test
    fun repositoryFiltersMasjidsByType() = runBlocking {
        val repository = LocalMasjidRepository(
            MasjidJsonDataSource(MasjidAssetReader { repositoryJson })
        )

        val prayerPoints = repository.getMasjidsByType("ho-chi-minh", MasjidType.PRAYER_POINT)

        assertEquals(1, prayerPoints.size)
        assertEquals("hcm-002", prayerPoints.first().id)
    }

    @Test
    fun repositoryFindsMasjidByStableId() = runBlocking {
        val repository = LocalMasjidRepository(
            MasjidJsonDataSource(MasjidAssetReader { repositoryJson })
        )

        val masjid = repository.getMasjidById("hcm-001")

        assertNotNull(masjid)
        assertEquals("Masjid Test", masjid?.name)
    }

    private val repositoryJson = """
        {
          "schemaVersion": 1,
          "province": {
            "id": "hcm",
            "name": "Ho Chi Minh City",
            "countryCode": "VN"
          },
          "managementOrganizations": [],
          "masjids": [
            {
              "id": "hcm-001",
              "name": "Masjid Test",
              "type": "MASJID",
              "location": {},
              "verification": {
                "status": "UNVERIFIED"
              }
            },
            {
              "id": "hcm-002",
              "name": "Prayer Point Test",
              "type": "PRAYER_POINT",
              "location": {},
              "verification": {
                "status": "UNVERIFIED"
              }
            }
          ]
        }
    """.trimIndent()
}
