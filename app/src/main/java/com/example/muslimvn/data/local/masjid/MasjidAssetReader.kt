package com.example.muslimvn.data.local.masjid

import android.content.Context

fun interface MasjidAssetReader {
    fun readText(path: String): String

    fun list(path: String): List<String> = emptyList()
}

class AndroidMasjidAssetReader(
    private val context: Context
) : MasjidAssetReader {
    override fun readText(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    override fun list(path: String): List<String> =
        context.assets.list(path)?.toList().orEmpty()
}
