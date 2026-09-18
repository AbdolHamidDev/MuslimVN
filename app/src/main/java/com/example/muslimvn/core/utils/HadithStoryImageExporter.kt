package com.example.muslimvn.core.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.muslimvn.domain.models.Hadith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Renders the story itself, rather than taking a screenshot containing viewer controls. */
object HadithStoryImageExporter {
    suspend fun saveToGallery(context: Context, story: Hadith): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = render(story)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "hadith_${story.id}_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MuslimVN")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create media item")
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                } ?: throw IOException("Could not open media item")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                uri
            } catch (error: Throwable) {
                context.contentResolver.delete(uri, null, null)
                throw error
            } finally { bitmap.recycle() }
        }
    }

    private fun render(story: Hadith): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL) }
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), intArrayOf(Color.rgb(23, 72, 63), Color.rgb(12, 32, 30), Color.rgb(18, 51, 46)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.rgb(189, 231, 210); paint.textSize = 31f; paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        canvas.drawText("LỜI NHẮC HÔM NAY", 88f, 190f, paint)

        val textSize = when {
            story.text.length <= 260 -> 66f
            story.text.length <= 520 -> 57f
            story.text.length <= 850 -> 47f
            else -> 42f
        }
        paint.color = Color.WHITE; paint.textSize = textSize; paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        val lines = wrap(story.text, paint, width - 176f)
        val lineHeight = textSize * 1.35f
        // Keep attribution/footer reserved. For exceptionally long text, fit the export
        // gracefully instead of allowing it to cover its source.
        val available = 1280f
        val scale = minOf(1f, available / (lines.size * lineHeight))
        paint.textSize *= scale
        val fittedLines = wrap(story.text, paint, width - 176f)
        val fittedLineHeight = paint.textSize * 1.35f
        var y = 390f + ((available - fittedLines.size * fittedLineHeight) / 2f).coerceAtLeast(0f)
        fittedLines.forEach { line -> canvas.drawText(line, 88f, y, paint); y += fittedLineHeight }

        paint.color = Color.rgb(213, 232, 223); paint.textSize = 34f; paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        story.attribution?.let { canvas.drawText(ellipsize(it, paint, width - 176f), 88f, 1650f, paint) }
        story.grade?.let { canvas.drawText(ellipsize(it, paint, width - 176f), 88f, 1700f, paint) }
        story.reference?.let { canvas.drawText(ellipsize(it, paint, width - 176f), 88f, 1750f, paint) }
        paint.color = Color.rgb(189, 231, 210); paint.textSize = 27f
        canvas.drawText("Nguồn: HadeethEnc.com", 88f, 1815f, paint)
        return bitmap
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> = buildList {
        text.split(Regex("\\s+")).filter { it.isNotBlank() }.fold("") { line, word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) candidate else { add(line); word }
        }.also { if (it.isNotBlank()) add(it) }
    }

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(value) <= maxWidth) return value
        val result = StringBuilder()
        for (character in value) {
            if (paint.measureText("$result$character…") > maxWidth) break
            result.append(character)
        }
        return "$result…"
    }
}
