/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import android.webkit.URLUtil
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpException
import org.readium.r2.shared.util.http.HttpException.Kind
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpRequest.Method
import org.readium.r2.shared.util.http.HttpResponse
import timber.log.Timber

/**
 * Fetches remote resources through HTTP.
 *
 * @param client HTTP client used to perform HTTP requests.
 * @param baseUrl Base URL from which relative HREF are served.
 */
class HttpFetcher(
    private val client: HttpClient,
    private val baseUrl: String? = null,
) : Fetcher {

    override suspend fun links(): List<Link> = emptyList()

    override fun get(link: Link): Resource {
        val url = link.toUrl(baseUrl)

        return if (url == null || !URLUtil.isNetworkUrl(url)) {
            val cause = IllegalArgumentException("Invalid HREF: ${link.href}, produced URL: $url")
            Timber.e(cause)
            FailureResource(link, error = Resource.Exception.BadRequest(cause = cause))
        } else {
            HttpResource(client, link, url)
        }
    }

    override suspend fun close() {}

    /** Provides access to an external URL. */
    private class HttpResource(
        private val client: HttpClient,
        private val link: Link,
        private val url: String,
    ) : Resource {

        override suspend fun link(): Link =
            headResponse()
                .map { link.copy(type = it.mediaType.toString()) }
                .getOrNull() ?: link

        override suspend fun length(): ResourceTry<Long> =
            headResponse().flatMap {
                val contentLength = it.contentLength
                return if (contentLength != null) {
                    Try.success(contentLength)
                } else {
                    Try.failure(Resource.Exception.Unavailable())
                }
            }

        override suspend fun close() {}

        override suspend fun read(range: LongRange?, consume: (ByteArray) -> Unit): ResourceTry<Unit> =
            client
                .progressiveDownload(
                    request = HttpRequest(url),
                    range = range,
                    consumeData = { data, _ -> consume(data) }
                )
                .map { }
                .mapFailure { Resource.Exception.wrapHttp(it) }

        /** Cached HEAD response to get the expected content length and other metadata. */
        private lateinit var _headResponse: ResourceTry<HttpResponse>

        private suspend fun headResponse(): ResourceTry<HttpResponse> {
            if (::_headResponse.isInitialized)
                return _headResponse

            _headResponse = client.fetch(HttpRequest(url, method = Method.HEAD))
                .map { it.response }
                .mapFailure { Resource.Exception.wrapHttp(it) }

            return _headResponse
        }

        private fun Resource.Exception.Companion.wrapHttp(e: HttpException): Resource.Exception =
            when (e.kind) {
                Kind.MalformedRequest, Kind.BadRequest ->
                    Resource.Exception.BadRequest(cause = e)
                Kind.Timeout, Kind.Offline ->
                    Resource.Exception.Unavailable(e)
                Kind.Unauthorized, Kind.Forbidden ->
                    Resource.Exception.Forbidden(e)
                Kind.NotFound ->
                    Resource.Exception.NotFound(e)
                Kind.Cancelled ->
                    Resource.Exception.Cancelled
                Kind.MalformedResponse, Kind.ClientError, Kind.ServerError, Kind.Other ->
                    Resource.Exception.Other(e)
            }

    }
}