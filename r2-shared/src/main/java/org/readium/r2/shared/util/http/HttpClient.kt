/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.util.mediatype.MediaType
import java.io.InputStream
import java.util.*

/**
 * An HTTP client performs HTTP requests.
 *
 * You may provide a custom implementation, or use the [DefaultHttpClient] one which relies on
 * native APIs.
 */
interface HttpClient {

    /**
     * Streams the resource from the given [request].
     */
    suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse>

    /**
     * Fetches the resource from the given [request].
     */
    suspend fun fetch(request: HttpRequest): HttpTry<HttpFetchResponse> =
        stream(request)
            .map { response ->
                val body = response.body.use { it.readBytes() }
                HttpFetchResponse(response.response, body)
            }

}

class HttpStreamResponse(
    val response: HttpResponse,
    val body: InputStream,
)

class HttpFetchResponse(
    val response: HttpResponse,
    val body: ByteArray,
)

/**
 * Represents a successful HTTP response received from a server.
 *
 * @param headers HTTP response headers, indexed by their name.
 * @param mediaType Media type sniffed from the `Content-Type` header and response body. Falls back
 *        on `application/octet-stream`.
 */
data class HttpResponse(
    val headers: Map<String, List<String>>,
    val mediaType: MediaType,
) {

    /**
     * Finds the first value of the first header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     */
    fun valueForHeader(name: String): String? =
        valuesForHeader(name).firstOrNull()

    /**
     * Finds all the values of the first header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     */
    fun valuesForHeader(name: String): List<String> {
        @Suppress("NAME_SHADOWING")
        val name = name.toLowerCase(Locale.ROOT)
        return headers
            .filterKeys { it.toLowerCase(Locale.ROOT) == name }
            .values
            .flatten()
    }

    /**
     * Indicates whether this server supports byte range requests.
     */
    val acceptsByteRanges: Boolean get() {
        return valueForHeader("Accept-Ranges")?.toLowerCase(Locale.ROOT) == "bytes"
            || valueForHeader("Content-Range")?.toLowerCase(Locale.ROOT)?.startsWith("bytes") == true
    }

    /**
     * The expected content length for this response, when known.
     *
     * Warning: For byte range requests, this will be the length of the chunk, not the full
     * resource.
     */
    val contentLength: Long? get() =
        valueForHeader("Content-Length")
            ?.toLongOrNull()
            ?.takeIf { it >= 0 }

}