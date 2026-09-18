package com.example.muslimvn.data.local.masjid

import android.content.Context
import com.example.muslimvn.domain.models.masjid.MasjidDatabase
import com.example.muslimvn.domain.models.masjid.MasjidDatabaseValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class MasjidJsonDataSource(
    private val assetReader: MasjidAssetReader,
    private val json: Json = defaultJson
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(AndroidMasjidAssetReader(context))

    suspend fun loadProvince(provinceId: String): MasjidDatabase? = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = assetReader.readText(provinceAssetPath(provinceId))
            parse(jsonText)
        }.getOrNull()
    }

    suspend fun getAvailableProvinceIds(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            assetReader.list(MASJID_ASSET_DIR)
                .filter { it.endsWith(".json") }
                .map { it.removeSuffix(".json") }
                .sorted()
        }.getOrDefault(emptyList())
    }

    fun parse(jsonText: String): MasjidDatabase? {
        return try {
            json.decodeFromString<MasjidDatabase>(jsonText)
                .takeIf { MasjidDatabaseValidator.validate(it).isValid }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private const val MASJID_ASSET_DIR = "masjids"

        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun provinceAssetPath(provinceId: String): String =
            "$MASJID_ASSET_DIR/$provinceId.json"
    }
}
