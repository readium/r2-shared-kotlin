package org.readium.r2.shared.util.http

import android.net.Uri
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Holds the information about an HTTP request performed by an [HttpClient].
 * @param url Address of the remote resource to request.
 * @param method HTTP method to use for the request.
 * @param headers Additional HTTP headers to use.
 * @param connectTimeout Timeout used when establishing a connection to the resource. A null timeout
 *        is interpreted as the default value, while a timeout of zero as an infinite timeout.
 * @param connectTimeout Timeout used when reading the input stream. A null timeout is interpreted
 *        as the default value, while a timeout of zero as an infinite timeout.
 * @param allowUserInteraction If true, the user might be presented with interactive dialogs, such
 *        as popping up an authentication dialog.
 */
@OptIn(ExperimentalTime::class)
data class HttpRequest(
    val url: String,
    val method: Method = Method.GET,
    val headers: Map<String, String> = mapOf(),
    val connectTimeout: Duration? = null,
    val readTimeout: Duration? = null,
    val allowUserInteraction: Boolean = false,
) {

    /** Supported HTTP methods. */
    enum class Method {
        GET, HEAD, POST, PUT;
    }

    companion object {
        operator fun invoke(build: Builder.() -> Unit): HttpRequest =
            Builder().apply(build).build()
    }

    class Builder(
        url: String = "",
        var method: Method = Method.GET,
        var headers: MutableMap<String, String> = mutableMapOf(),
        var connectTimeout: Duration? = null,
        var readTimeout: Duration? = null,
        var allowUserInteraction: Boolean = false,
    ) {

        var url: String
            get() = uriBuilder.build().toString()
            set(value) { uriBuilder = Uri.parse(value).buildUpon() }

        init {
            this.url = url
        }

        private var uriBuilder: Uri.Builder = Uri.Builder()

        fun appendQueryParameter(key: String, value: String?) {
            if (value != null) {
                uriBuilder.appendQueryParameter(key, value)
            }
        }

        fun appendQueryParameters(params: Map<String, String?>) {
            for ((key, value) in params) {
                appendQueryParameter(key, value)
            }
        }

        fun setHeader(key: String, value: String) {
            headers[key] = value
        }

        fun build(): HttpRequest = HttpRequest(
            url = url,
            method = method,
            headers = headers.toMap(),
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            allowUserInteraction = allowUserInteraction,
        )

    }

}
