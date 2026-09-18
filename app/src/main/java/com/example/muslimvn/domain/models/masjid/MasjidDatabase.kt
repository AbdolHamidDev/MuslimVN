package com.example.muslimvn.domain.models.masjid

import kotlinx.serialization.Serializable

@Serializable
data class MasjidDatabase(
    val schemaVersion: Int,
    val province: Province,
    val managementOrganizations: List<ManagementOrganization> = emptyList(),
    val masjids: List<Masjid> = emptyList()
)

@Serializable
data class Province(
    val id: String,
    val name: String,
    val countryCode: String
)

@Serializable
data class ManagementOrganization(
    val id: String,
    val name: String,
    val type: ManagementOrganizationType,
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null
)

@Serializable
enum class ManagementOrganizationType {
    PROVINCIAL_ISLAMIC_REPRESENTATIVE_BOARD,
    LOCAL_ISLAMIC_ORGANIZATION,
    MOSQUE_MANAGEMENT_BOARD,
    OTHER
}

@Serializable
data class Masjid(
    val id: String,
    val name: String,
    val nativeName: String? = null,
    val aliases: List<String> = emptyList(),
    val type: MasjidType,
    val location: Location,
    val contact: Contact? = null,
    val prayer: PrayerInfo? = null,
    val facilities: Facilities? = null,
    val services: Services? = null,
    val management: Management? = null,
    val media: Media? = null,
    val verification: Verification,
    val notes: String? = null
)

@Serializable
enum class MasjidType {
    MASJID,
    TIEU_THANG_DUONG,
    PRAYER_POINT,
    OTHER
}

@Serializable
data class Location(
    val address: String? = null,
    val province: String? = null,
    val district: String? = null,
    val commune: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class Contact(
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null
)

@Serializable
data class PrayerInfo(
    val jummah: List<String> = emptyList(),
    val eidPrayer: List<String> = emptyList()
)

@Serializable
data class Facilities(
    val womenPrayerArea: Boolean? = null,
    val wudu: Boolean? = null,
    val parking: Boolean? = null,
    val quran: Boolean? = null,
    val quranClass: Boolean? = null
)

@Serializable
data class Services(
    val iftarRamadan: Boolean? = null,
    val islamicClasses: Boolean? = null,
    val funeralPrayer: Boolean? = null,
    val nikah: Boolean? = null
)

@Serializable
data class Management(
    val organizationId: String? = null,
    val boardName: String? = null
)

@Serializable
data class Media(
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList()
)

@Serializable
data class Verification(
    val status: VerificationStatus,
    val sources: List<DataSource> = emptyList(),
    val lastVerifiedAt: String? = null
)

@Serializable
enum class VerificationStatus {
    OFFICIAL,
    COMMUNITY,
    MAP,
    UNVERIFIED
}

@Serializable
data class DataSource(
    val name: String,
    val url: String? = null
)
