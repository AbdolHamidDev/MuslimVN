package com.example.muslimvn.presentation.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfViewer(
    pdfAssetName: String,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val bitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    // Copy PDF from assets to cache for Renderer
    LaunchedEffect(pdfAssetName) {
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "temp_azkar.pdf")
            if (!file.exists()) {
                context.assets.open(pdfAssetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(input)
            pdfRenderer = renderer
            pageCount = renderer.pageCount
        }
    }

    // Scroll to initial page when set
    LaunchedEffect(initialPage) {
        if (initialPage in 0 until pageCount) {
            lazyListState.scrollToItem(initialPage)
        }
    }

    if (pdfRenderer == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(pageCount) { pageIndex ->
                val bitmap = bitmaps[pageIndex] ?: remember(pageIndex) {
                    val page = pdfRenderer?.openPage(pageIndex)
                    val b = Bitmap.createBitmap(
                        page?.width ?: 100,
                        page?.height ?: 100,
                        Bitmap.Config.ARGB_8888
                    )
                    // Set white background for PDF page
                    b.eraseColor(android.graphics.Color.WHITE)
                    page?.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page?.close()
                    bitmaps[pageIndex] = b
                    b
                }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Trang $pageIndex",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}
