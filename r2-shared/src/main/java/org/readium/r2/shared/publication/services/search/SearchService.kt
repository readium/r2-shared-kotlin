/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

typealias SearchTry<SuccessT> = Try<SuccessT, SearchException>

/**
 * Represents an error which might occur during a search activity.
 */
sealed class SearchException(content: Content, cause: Throwable? = null) : UserException(content, cause) {
    constructor(@StringRes userMessageId: Int, vararg args: Any, cause: Throwable? = null)
        : this(Content(userMessageId, *args), cause)
    constructor(cause: UserException)
        : this(Content(cause), cause)

    /**
     * The publication is not searchable.
     */
    object PublicationNotSearchable : SearchException(R.string.r2_shared_search_exception_publication_not_searchable)

    /**
     * The provided search query cannot be handled by the service.
     */
    class BadQuery(cause: UserException) : SearchException(cause)

    /**
     * An error occurred while accessing one of the publication's resources.
     */
    class ResourceError(cause: Resource.Exception) : SearchException(cause)

    /**
     * An error occurred while performing an HTTP request.
     */
    class NetworkError(cause: HttpException) : SearchException(cause)

    /**
     * The search was cancelled by the caller.
     *
     * For example, when a coroutine or a network request is cancelled.
     */
    object Cancelled : SearchException(R.string.r2_shared_search_exception_cancelled)

    /** For any other custom service error. */
    class Other(cause: Throwable) : SearchException(R.string.r2_shared_search_exception_other, cause = cause)

    companion object {
        fun wrap(e: Throwable): SearchException =
            when (e) {
                is SearchException -> e
                is CancellationException, is Resource.Exception.Cancelled -> Cancelled
                is Resource.Exception -> ResourceError(e)
                is HttpException ->
                    if (e.kind == HttpException.Kind.Cancelled) {
                        Cancelled
                    } else {
                        NetworkError(e)
                    }
                else -> Other(e)
            }
    }
}

/**
 * Provides a way to search terms in a publication.
 */
interface SearchService : Publication.Service {

    /**
     * Represents an option and its current value supported by a service.
     *
     * Implementation note: key and rawValue are opened to be able to use @Parcelize in Custom case.
     */
    sealed class Option(open val key: String, protected open val rawValue: Any?) : Parcelable {

        val value: String? get() =
            rawValue?.toString()?.takeIf { it.isNotBlank() }

        /**
         * Whether the search will differentiate between capital and lower-case letters.
         */
        @Parcelize class CaseSensitive(val on: Boolean) : Option("case-sensitive", on)

        /**
         * Whether the search will differentiate between letters with accents or not.
         */
        @Parcelize class DiacriticSensitive(val on: Boolean) : Option("diacritic-sensitive", on)

        /**
         * Whether the query terms will match full words and not parts of a word.
         */
        @Parcelize class WholeWord(val on: Boolean) : Option("whole-word", on)

        /**
         * Matches results similar but not identical to the query, such as reordered or words with a
         * related stem. For example, "banana split" would match "I love banana split" but also
         * "splitting all the bananas". When close variants are enabled, surround terms with double
         * quotes for an exact match.
         */
        @Parcelize class CloseVariants(val on: Boolean) : Option("close-variants", on)

        /**
         * Matches results with typos or similar spelling.
         *
         * See https://en.wikipedia.org/wiki/Approximate_string_matching
         */
        @Parcelize class Fuzzy(val on: Boolean) : Option("fuzzy", on)

        /**
         * A custom option implemented by a Search Service which is not officially recognized by
         * Readium.
         */
        @Parcelize class Custom(override val key: String, override val rawValue: String) : Option(key, rawValue)
    }

    /**
     * All search options available for this service.
     *
     * Also holds the default value for these options, which can be useful to setup the views
     * in the search interface. If an option is missing when calling search(), its value is assumed
     * to be the default one.
     */
    val options: Set<Option>

    /**
     * Starts a new search through the publication content, with the given [query].
     */
    suspend fun search(query: String, options: Set<Option> = emptySet()): SearchTry<SearchIterator>
}

/**
 * Indicates whether the content of this publication can be searched.
 */
val Publication.isSearchable get() =
    findService(SearchService::class) != null

/**
 * All search options available for this service.
 *
 * Also holds the default value for these options, which can be useful to setup the views
 * in the search interface. If an option is missing when calling search(), its value is assumed
 * to be the default one.
 */
val Publication.searchOptions: Set<SearchService.Option> get() =
    findService(SearchService::class)?.options ?: emptySet()

/**
 * Starts a new search through the publication content, with the given [query].
 */
suspend fun Publication.search(query: String, options: Set<SearchService.Option> = emptySet()): SearchTry<SearchIterator> =
    findService(SearchService::class)?.search(query, options)
        ?: Try.failure(SearchException.PublicationNotSearchable)

/** Factory to build a [SearchService] */
var Publication.ServicesBuilder.searchServiceFactory: ServiceFactory?
    get() = get(SearchService::class)
    set(value) = set(SearchService::class, value)

/**
 * Iterates through search results.
 */
interface SearchIterator : SuspendingCloseable {

    /**
     * Number of matches for this search, if known.
     *
     * Depending on the search algorithm, it may not be possible to know the result count until
     * reaching the end of the publication.
     */
    val resultCount: Int? get() = null

    /**
     * Retrieves the next page of results.
     *
     * @return Null when reaching the end of the publication, or an error in case of failure.
     */
    suspend fun next(): SearchTry<LocatorCollection?>

    /**
     * Closes any resources allocated for the search query, such as a cursor.
     * To be called when the user dismisses the search.
     */
    override suspend fun close() {}

    /**
     * Performs the given operation on each result page of this [SearchIterator].
     */
    suspend fun forEach(action: (LocatorCollection) -> Unit): SearchTry<Unit> {
        while (true) {
            val res = next()
            res
                .onSuccess { locators ->
                    if (locators != null) {
                        action(locators)
                    } else {
                        return Try.success(Unit)
                    }
                }
                .onFailure {
                    return Try.failure(it)
                }
        }
    }
}

inline fun <reified T: SearchService.Option> Set<SearchService.Option>.get(): T? =
    firstOrNull { it is T } as? T