package com.example.muslimvn.data.repository

import android.content.Context
import com.example.muslimvn.data.remote.HadeethEncApiService
import com.example.muslimvn.domain.models.Hadith
import com.example.muslimvn.domain.models.cleanCurlyBraces
import com.example.muslimvn.domain.repository.HadithRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val api: HadeethEncApiService,
    private val gson: Gson
) : HadithRepository {
    private val preferences = context.getSharedPreferences("daily_hadith_cache", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private val loaded = linkedMapOf<String, Hadith>()
    private val categoryIds = listOf("5", "3", "4", "1", "2", "6", "7")
    private var categoryIndex = 0
    private var page = 1

    init { restoreCache() }

    override fun cachedHadiths(): List<Hadith> = loaded.values.toList()

    override suspend fun loadNextBatch(batchSize: Int): Result<List<Hadith>> = lock.withLock {
        runCatching {
            val before = loaded.size
            var stalledPages = 0
            // A category page has IDs only; fetch its details concurrently so each batch is
            // ready before the viewer reaches it, rather than doing a request per swipe.
            while (loaded.size - before < batchSize) {
                val beforePage = loaded.size
                val response = api.getHadeeths(categoryId = categoryIds[categoryIndex], page = page, perPage = batchSize)
                page += 1
                if (response.meta?.lastPage?.let { page > it } == true || response.data.isEmpty()) {
                    categoryIndex = (categoryIndex + 1) % categoryIds.size
                    page = 1
                }
                val details = coroutineScope {
                    response.data.map { item -> async { runCatching { api.getHadeeth(item.id) }.getOrNull() } }.awaitAll()
                }
                details.filterNotNull().mapNotNull { detail -> detail.toHadith() }.forEach { loaded.putIfAbsent(it.id, it) }
                stalledPages = if (loaded.size == beforePage) stalledPages + 1 else 0
                // A transient detail-endpoint problem must not turn a Home load into a loop.
                if (response.data.isEmpty() || stalledPages >= 2) break
            }
            persistCache()
            loaded.values.drop(before).take(batchSize)
        }
    }

    private fun com.example.muslimvn.data.remote.model.HadeethDetailDto.toHadith(): Hadith? {
        val hadithText = text?.cleanCurlyBraces()?.trim().orEmpty()
        if (hadithText.isBlank()) return null
        return Hadith(
            id = id,
            title = title?.cleanCurlyBraces()?.trim().orEmpty(),
            text = hadithText,
            attribution = attribution?.cleanCurlyBraces()?.trim()?.takeIf { it.isNotBlank() },
            grade = grade?.cleanCurlyBraces()?.trim()?.takeIf { it.isNotBlank() },
            explanation = explanation?.cleanCurlyBraces()?.trim()?.takeIf { it.isNotBlank() },
            reference = null, category = categories.firstOrNull(), language = "vi"
        )
    }

    private fun restoreCache() {
        val json = preferences.getString(CACHE_KEY, null) ?: return
        val cachedAt = preferences.getLong(CACHE_TIME_KEY, 0)
        if (System.currentTimeMillis() - cachedAt > CACHE_MAX_AGE_MS) return
        runCatching {
            val type = object : TypeToken<List<Hadith>>() {}.type
            gson.fromJson<List<Hadith>>(json, type).orEmpty().forEach { item ->
                val cleanedItem = item.copy(
                    title = item.title.cleanCurlyBraces(),
                    text = item.text.cleanCurlyBraces(),
                    attribution = item.attribution?.cleanCurlyBraces(),
                    grade = item.grade?.cleanCurlyBraces(),
                    explanation = item.explanation?.cleanCurlyBraces()
                )
                loaded[cleanedItem.id] = cleanedItem
            }
            categoryIndex = preferences.getInt(CATEGORY_INDEX_KEY, 0).coerceIn(0, categoryIds.lastIndex)
            page = preferences.getInt(PAGE_KEY, 1).coerceAtLeast(1)
        }
    }

    private fun persistCache() {
        // Keep this feature's disk cache bounded; it is only a reading cache, not a database.
        preferences.edit().putString(CACHE_KEY, gson.toJson(loaded.values.toList().takeLast(MAX_DISK_ITEMS)))
            .putLong(CACHE_TIME_KEY, System.currentTimeMillis())
            .putInt(CATEGORY_INDEX_KEY, categoryIndex)
            .putInt(PAGE_KEY, page)
            .apply()
    }

    private companion object {
        const val CACHE_KEY = "items"
        const val CACHE_TIME_KEY = "updated_at"
        const val CATEGORY_INDEX_KEY = "category_index"
        const val PAGE_KEY = "next_page"
        const val MAX_DISK_ITEMS = 80
        const val CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }
}
