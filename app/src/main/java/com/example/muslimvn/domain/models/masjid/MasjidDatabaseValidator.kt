package com.example.muslimvn.domain.models.masjid

object MasjidDatabaseValidator {

    private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun validate(database: MasjidDatabase): MasjidValidationResult {
        val errors = buildList {
            validateDuplicateIds(database, this)
            validateCoordinates(database, this)
            validatePrayerTimes(database, this)
            validateOrganizationReferences(database, this)
        }
        return MasjidValidationResult(errors)
    }

    private fun validateDuplicateIds(
        database: MasjidDatabase,
        errors: MutableList<MasjidValidationError>
    ) {
        database.masjids
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id ->
                errors += MasjidValidationError("Duplicate masjid id: $id")
            }

        database.managementOrganizations
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id ->
                errors += MasjidValidationError("Duplicate organization id: $id")
            }
    }

    private fun validateCoordinates(
        database: MasjidDatabase,
        errors: MutableList<MasjidValidationError>
    ) {
        database.masjids.forEach { masjid ->
            masjid.location.latitude?.let { latitude ->
                if (latitude !in -90.0..90.0) {
                    errors += MasjidValidationError("Invalid latitude for ${masjid.id}: $latitude")
                }
            }
            masjid.location.longitude?.let { longitude ->
                if (longitude !in -180.0..180.0) {
                    errors += MasjidValidationError("Invalid longitude for ${masjid.id}: $longitude")
                }
            }
        }
    }

    private fun validatePrayerTimes(
        database: MasjidDatabase,
        errors: MutableList<MasjidValidationError>
    ) {
        database.masjids.forEach { masjid ->
            masjid.prayer?.jummah.orEmpty()
                .filterNot { timeRegex.matches(it) }
                .forEach { time ->
                    errors += MasjidValidationError("Invalid Jumu'ah time for ${masjid.id}: $time")
                }

            masjid.prayer?.eidPrayer.orEmpty()
                .filterNot { timeRegex.matches(it) }
                .forEach { time ->
                    errors += MasjidValidationError("Invalid Eid prayer time for ${masjid.id}: $time")
                }
        }
    }

    private fun validateOrganizationReferences(
        database: MasjidDatabase,
        errors: MutableList<MasjidValidationError>
    ) {
        val organizationIds = database.managementOrganizations.map { it.id }.toSet()
        database.masjids.forEach { masjid ->
            val organizationId = masjid.management?.organizationId
            if (organizationId != null && organizationId !in organizationIds) {
                errors += MasjidValidationError(
                    "Missing organization reference for ${masjid.id}: $organizationId"
                )
            }
        }
    }
}

data class MasjidValidationResult(
    val errors: List<MasjidValidationError>
) {
    val isValid: Boolean = errors.isEmpty()
}

data class MasjidValidationError(
    val message: String
)
