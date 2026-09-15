package com.example.muslimvn.core.network

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val okHttpRequestBuilder = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
            .apply {
                headers.forEach { (name, values) ->
                    if (name.equals("Accept-Language", ignoreCase = true)) return@forEach
                    values.forEach { value ->
                        addHeader(name, value)
                    }
                }
            }

        if (method == "GET") {
            okHttpRequestBuilder.get()
        } else if (method == "POST") {
            okHttpRequestBuilder.post(dataToSend?.toRequestBody() ?: "".toRequestBody())
        }

        val okHttpResponse = client.newCall(okHttpRequestBuilder.build()).execute()

        if (okHttpResponse.code >= 400) {
            val responseCode = okHttpResponse.code
            okHttpResponse.close()
            throw IOException("Invalid response code: $responseCode")
        }

        val body = okHttpResponse.body?.string()
        val responseHeaders = okHttpResponse.headers.toMultimap()

        return Response(okHttpResponse.code, okHttpResponse.message, responseHeaders, body, url)
    }
}
