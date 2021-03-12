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
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.ExperimentalTime

/**
 * An implementation of [HttpClient] using the native [HttpURLConnection].
 */
@OptIn(ExperimentalTime::class)
class DefaultHttpClient : HttpClient {

    @Suppress("BlockingMethodInNonBlockingContext") // We are using Dispatchers.IO but we still get this warning...
    override suspend fun fetch(request: HttpRequest): HttpTry<HttpFetchResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(request.url)

            val connection = (url.openConnection() as HttpURLConnection)
            connection.requestMethod = request.method.name
            if (request.readTimeout != null) {
                connection.readTimeout = request.readTimeout.toLongMilliseconds().toInt()
            }
            if (request.connectTimeout != null) {
                connection.connectTimeout = request.connectTimeout.toLongMilliseconds().toInt()
            }

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
                        headers = connection.headerFields,
                        mediaType = mediaType ?: MediaType.BINARY,
                    ),
                    body = body,
                ))

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            Timber.e(e)
            Try.failure(HttpException.wrap(e))
        }
    }

    override suspend fun progressiveDownload(request: HttpRequest, range: LongRange?, receiveResponse: ((HttpResponse) -> Void)?, consumeData: (chunk: ByteArray, progress: Double?) -> Unit): HttpTry<HttpResponse> {
        TODO("Not yet implemented")
    }

}