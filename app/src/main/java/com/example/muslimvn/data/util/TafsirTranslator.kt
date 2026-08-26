package com.example.muslimvn.data.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TafsirTranslator @Inject constructor() {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.VIETNAMESE)
        .build()

    private val translator: Translator = Translation.getClient(options)
    private val modelManager = RemoteModelManager.getInstance()
    private val viModel = TranslateRemoteModel.Builder(TranslateLanguage.VIETNAMESE).build()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    suspend fun isModelDownloaded(): Boolean {
        return modelManager.isModelDownloaded(viModel).await()
    }

    suspend fun downloadModel(): Boolean {
        _isDownloading.value = true
        return try {
            val conditions = DownloadConditions.Builder()
                .build() // In real app, might want to check for wifi
            translator.downloadModelIfNeeded(conditions).await()
            true
        } catch (e: Exception) {
            false
        } finally {
            _isDownloading.value = false
        }
    }

    suspend fun translate(text: String): String? {
        if (!isModelDownloaded()) {
            val success = downloadModel()
            if (!success) return null
        }
        
        return try {
            translator.translate(text).await()
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        translator.close()
    }
}
