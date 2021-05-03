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
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Ref
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

/**
 * A basic implementation of [SearchService] iterating through the content of Publication's
 * resources and delegating the actual search to media-type-specific searchers.
 */
class SimpleSearchService private constructor(
    private val searchers: List<Searcher>,
    private val publication: Ref<Publication>,
) : SearchService {

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
            val searcher = searcherForMediaType(link.mediaType) ?: return next()

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
            val searcher = searchers.firstOrNull { it.supportedMediaTypes.contains(mediaType) }
            if (searcher == null) {
                Timber.w("No searcher registered for media type $mediaType")
            }
            return searcher
        }
    }

    companion object {
        fun createFactory(searchers: List<Searcher>): (Publication.Service.Context) -> SimpleSearchService =
            { context -> SimpleSearchService(searchers, context.publication) }
    }

    /**
     * Resource searcher specific to a media type.
     */
    interface Searcher {
        val supportedMediaTypes: List<MediaType>

        suspend fun search(publication: Publication, resource: Resource, query: String, options: Set<SearchService.Option>): SearchTry<LocatorCollection>
    }

    /**
     * Base searcher provides utilities for Searcher implementations.
     */
    abstract class BaseSearcher(private val surroundingContextLength: Int = 70) : Searcher {

        abstract suspend fun textOfResource(resource: Resource): String

        override suspend fun search(publication: Publication, resource: Resource, query: String, options: Set<SearchService.Option>): SearchTry<LocatorCollection> {
            return try {
                withContext(Dispatchers.IO) {
                    val content = textOfResource(resource)
                    val locators = findLocators(query, content, resource.link())
                    Try.success(LocatorCollection(locators = locators))
                }

            } catch (e: Exception) {
                Try.failure(SearchException.wrap(e))
            }
        }

        private fun findLocators(query: String, content: String, link: Link): List<Locator> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                return emptyList() // FIXME

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

            return locators
        }
    }

    /**
     * A [SimpleSearchService.Searcher] implementation for (X)HTML resources.
     */
    class HtmlSearcher : BaseSearcher() {

        override val supportedMediaTypes: List<MediaType>
            get() = listOf(MediaType.HTML, MediaType.XHTML)

        override suspend fun textOfResource(resource: Resource): String {
            val html = resource.readAsString(charset = null).getOrThrow()
            return Jsoup.parse(html).body().text()
        }
    }

}