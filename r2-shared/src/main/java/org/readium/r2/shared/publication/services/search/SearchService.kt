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
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpException

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
interface SearchService {

    /**
     * Represents a feature supported by the Search Service.
     * For example, whether the search supports boolean operators.
     */
    sealed class Capability(val uri: String) : Parcelable {
        @Parcelize object CaseInsensitive : Capability("case-insensitive")
        @Parcelize class MinimumQueryLength(val int: Int) : Capability("min-query-length")
    }

    sealed class Option(val uri: String) : Parcelable {
        @Parcelize object CaseInsensitive : Option("case-insensitive")
        @Parcelize object WholeWord : Option("whole-word")
    }

    /**
     * Search capabilities supported by this service.
     */
    val capabilities: Set<Capability>

    /**
     * All search options available for this service.
     *
     * Used to display the user options in the search interface.
     */
    val supportedOptions: Set<Option>

    /**
     * Default search options enabled with this service.
     *
     * Used to pre-check options in the search interface.
     */
    val defaultOptions: Set<Option>

    /**
     * Starts a new search through the publication content, with the given [query].
     */
    suspend fun search(query: String, options: Set<Option> = emptySet()): SearchTry<SearchIterator>
}

/**
 * Iterates through search results.
 */
interface SearchIterator {

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
    suspend fun close() {}
}