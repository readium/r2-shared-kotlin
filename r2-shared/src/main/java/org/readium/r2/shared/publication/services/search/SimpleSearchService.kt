/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.icu.text.StringSearch
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.toLocator
import org.readium.r2.shared.util.Ref
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

/**
 * A basic implementation of [SearchService] iterating through the content of Publication's
 * resources and delegating the actual search to media-type-specific searchers.
 */
class SimpleSearchService private constructor(
    private val searchers: Map<List<MediaType>, Searcher>,
    private val publication: Ref<Publication>,
) : SearchService {

    /**
     * Resource searcher specific to a media type.
     */
    interface Searcher {
        suspend fun search(publication: Publication, resource: Resource, query: String, options: Set<SearchService.Option>): SearchTry<LocatorCollection>
    }

    override val capabilities: Set<SearchService.Capability>
        get() = emptySet()
    override val supportedOptions: Set<SearchService.Option>
        get() = emptySet()
    override val defaultOptions: Set<SearchService.Option>
        get() = emptySet()

    override suspend fun search(query: String, options: Set<SearchService.Option>): SearchTry<SearchIterator> =
        Try.success(Iterator(query, options))

    private inner class Iterator(val query: String, val options: Set<SearchService.Option>) : SearchIterator {
        /**
         * Index of the last reading order resource searched in.
         */
        private var index = -1

        override suspend fun next(): SearchTry<LocatorCollection?> {
            val publication = publication()
            if (publication == null || index >= publication.readingOrder.count() - 1) {
                return Try.success(null)
            }

            index += 1

            val link = publication.readingOrder[index]
            val searcher = searcherForMediaType(link.mediaType) ?:
                return next()

            val resource = publication.get(link)
            val result = searcher.search(publication, resource, query, options)

            // If no occurrences were found in the current resource, skip to the next one
            // automatically.
            if (result.getOrNull()?.locators?.isEmpty() == true) {
                return next()
            }

            return result
        }

        private fun searcherForMediaType(mediaType: MediaType): Searcher? {
            for ((mediaTypes, searcher) in searchers) {
                if (mediaTypes.contains(mediaType)) {
                    return searcher
                }
            }

            Timber.w("No searcher registered for media type $mediaType")
            return null
        }
    }

    companion object {
        val defaultSearchers: Map<List<MediaType>, Searcher> = mapOf(
            listOf(MediaType.HTML, MediaType.XHTML) to HtmlSearcher()
        )

        fun createFactory(searchers: Map<List<MediaType>, Searcher> = defaultSearchers) : (Publication.Service.Context) -> SimpleSearchService =
            { context -> SimpleSearchService(searchers, context.publication) }
    }
}

/**
 * A [SimpleSearchService.Searcher] implementation for (X)HTML resources.
 */
class HtmlSearcher(private val surroundingContextLength: Int = 50) : SimpleSearchService.Searcher {

    override suspend fun search(publication: Publication, resource: Resource, query: String, options: Set<SearchService.Option>): SearchTry<LocatorCollection> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return Try.success(LocatorCollection())

        return try {
            withContext(Dispatchers.IO) {
                val html = resource.readAsString(charset = null).getOrThrow()
                val content = Jsoup.parse(html).body().text()

                val link = resource.link()
                val locators = mutableListOf<Locator>()

                val iter = StringSearch(query, content)
                var start = iter.first()
                while (start != android.icu.text.SearchIterator.DONE) {
                    val end = start + iter.matchLength

                    var locator = link.toLocator()
                    locator = locator.copy(
                        locations = locator.locations.copy(
                            progression = start.toDouble() / content.length.toDouble(),
                        ),
                        text = Locator.Text(
                            highlight = content.substring(start until end),
                            before = content.substring((start - surroundingContextLength).coerceAtLeast(0) until start),
                            after = content.substring(end until (end + surroundingContextLength).coerceAtMost(content.length)),
                        )
                    )
                    locators.add(locator)

                    start = iter.next()
                }

                Try.success(LocatorCollection(locators = locators))
            }

        } catch (e: Exception) {
            Try.failure(SearchException.wrap(e))
        }
    }
}