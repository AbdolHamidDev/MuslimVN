package com.example.muslimvn.core.network

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.toRequestBody()
        val okHttpRequestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(method, requestBody)
            // Let NewPipe supply its localization and service-specific headers.
            // This is the same header precedence used by NewPipe's DownloaderImpl.
            .addHeader("User-Agent", USER_AGENT)
            .apply {
                headers.forEach { (name, values) ->
                    removeHeader(name)
                    values.forEach { value ->
                        addHeader(name, value)
                    }
                }
            }

        client.newCall(okHttpRequestBuilder.build()).execute().use { okHttpResponse ->
            if (okHttpResponse.code == 429) {
                throw ReCaptchaException("YouTube yêu cầu xác minh reCAPTCHA", url)
            }

            return Response(
                okHttpResponse.code,
                okHttpResponse.message,
                okHttpResponse.headers.toMultimap(),
                okHttpResponse.body?.string(),
                okHttpResponse.request.url.toString()
            )
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}
