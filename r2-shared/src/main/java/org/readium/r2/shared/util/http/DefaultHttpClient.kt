/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpRequest.Method
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.sniffMediaType
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.ExperimentalTime

/**
 * An implementation of [HttpClient] using the native [HttpURLConnection].
 */
class DefaultHttpClient : HttpClient {

    @Suppress("BlockingMethodInNonBlockingContext") // We are using Dispatchers.IO but we still get this warning...
    override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> = withContext(Dispatchers.IO) {
        Timber.i("HTTP ${request.method.name} ${request.url}, headers: ${request.headers}")

        try {
            val connection = request.toHttpURLConnection()

            val statusCode = connection.responseCode
            HttpException.Kind.ofStatusCode(statusCode)?.let { kind ->
                // It was a HEAD request? We need to query the resource again to get the error body.
                // The body is needed for example when the response is an OPDS Authentication
                // Document.
                if (request.method == Method.HEAD) {
                    return@withContext stream(request.copy(method = Method.GET))
                }

                // Reads the full body, since it might contain an error representation such as
                // JSON Problem Details or OPDS Authentication Document
                val body = connection.inputStream.use { it.readBytes() }
                val mediaType = connection.sniffMediaType(bytes = { body })
                throw HttpException(kind, mediaType, body)
            }

            Try.success(HttpStreamResponse(
                response = HttpResponse(
                    headers = connection.safeHeaders,
                    mediaType = connection.sniffMediaType() ?: MediaType.BINARY,
                ),
                body = connection.inputStream,
            ))

        } catch (e: Exception) {
            if (e !is CancellationException) {
                Timber.e(e, "HTTP request failed ${request.url}")
            }
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
