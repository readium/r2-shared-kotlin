/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpRequest.Method
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.sniffMediaType
import timber.log.Timber
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.ExperimentalTime

/**
 * An implementation of [HttpClient] using the native [HttpURLConnection].
 */
class DefaultHttpClient : HttpClient {

    @Suppress("BlockingMethodInNonBlockingContext") // We are using Dispatchers.IO but we still get this warning...
    override suspend fun fetch(request: HttpRequest): HttpTry<HttpFetchResponse> = withContext(Dispatchers.IO) {
        Timber.i("Fetch (${request.method.name}) ${request.url}, headers: ${request.headers}")

        try {
            val connection = request.toHttpURLConnection()

            try {
                val statusCode = connection.responseCode
                val body = connection.inputStream.use { it.readBytes() }
                val mediaType = connection.sniffMediaType(bytes = { body })

                // Make sure the request was not cancelled.
                ensureActive()

                val exception = HttpException(statusCode, mediaType, body)
                if (exception != null) {
                    // It was a HEAD request? We need to query the resource again to get the error body.
                    // The body is needed for example when the response is an OPDS Authentication
                    // Document.
                    if (request.method == Method.HEAD) {
                        return@withContext fetch(request.copy(method = Method.GET))
                    }

                    throw exception
                }

                Try.success(HttpFetchResponse(
                    response = HttpResponse(
                        headers = connection.safeHeaders,
                        mediaType = mediaType ?: MediaType.BINARY,
                    ),
                    body = body,
                ))

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch ${request.url}")
            Try.failure(HttpException.wrap(e))
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext") // We are using Dispatchers.IO but we still get this warning...
    override suspend fun progressiveDownload(
        request: HttpRequest,
        range: LongRange?,
        receiveResponse: ((HttpResponse) -> Void)?,
        consumeData: (chunk: ByteArray, progress: Double?) -> Unit
    ): HttpTry<HttpResponse> = withContext(Dispatchers.IO) {
        try {
            @Suppress("NAME_SHADOWING") var request = request
            if (range != null) {
                request = request.buildUpon()
                    .setHeader("Range", "bytes=${range.first}-${range.last}")
                    .build()
            }

            Timber.i("Download (progressive) ${request.url}, headers: ${request.headers}")

            val connection = request.toHttpURLConnection()

            try {
                val statusCode = connection.responseCode
                HttpException.Kind.ofStatusCode(statusCode)?.let { kind ->
                    // Reads the full body, since it might contain an error representation such as
                    // JSON Problem Details or OPDS Authentication Document
                    val body = connection.inputStream.use { it.readBytes() }
                    val mediaType = connection.sniffMediaType(bytes = { body })
                    throw HttpException(kind, mediaType, body)
                }

                val response = HttpResponse(
                    headers = connection.safeHeaders,
                    mediaType = connection.sniffMediaType() ?: MediaType.BINARY,
                )
                receiveResponse?.invoke(response)

                if (range != null && !response.acceptsByteRanges) {
                    val e = Exception("Progressive download using ranges requires the remote HTTP server to support byte range requests: ${request.url}")
                    Timber.e(e)
                    throw e
                }

                var readLength = 0L
                val expectedLength = (
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        connection.contentLengthLong.toDouble()
                    } else {
                        connection.contentLength.toDouble()
                    }
                ).takeIf { it > 0 }

                BufferedInputStream(connection.inputStream).use { input ->
                    val chunk = ByteArray(2048)
                    var n: Int
                    while (-1 != input.read(chunk).also { n = it }) {
                        // Make sure the request was not cancelled.
                        ensureActive()

                        readLength += n
                        val progress = expectedLength?.let { readLength / it }
                        consumeData(chunk.copyOfRange(0, n), progress)
                    }
                }

                Try.success(response)

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to download ${request.url}")
            Try.failure(HttpException.wrap(e))
        }
    }

}

@OptIn(ExperimentalTime::class)
private fun HttpRequest.toHttpURLConnection(): HttpURLConnection {
    val url = URL(url)
    val connection = (url.openConnection() as HttpURLConnection)
    connection.requestMethod = method.name
    if (readTimeout != null) {
        connection.readTimeout = readTimeout.toLongMilliseconds().toInt()
    }
    if (connectTimeout != null) {
        connection.connectTimeout = connectTimeout.toLongMilliseconds().toInt()
    }
    connection.allowUserInteraction = allowUserInteraction

    for ((k, v) in headers) {
        connection.setRequestProperty(k, v)
    }

    return connection
}

private val HttpURLConnection.safeHeaders: Map<String, List<String>> get() =
    headerFields.filterNot { (key, value) ->
        // In practice, I found that some header names are null despite the force unwrapping.
        @Suppress("SENSELESS_COMPARISON")
        key == null || value == null
    }
