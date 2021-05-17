/*
 * Copyight 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.fetcher.DefaultResourceContentExtractorFactory
import org.readium.r2.shared.fetcher.ResourceContentExtractor
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.publication.services.search.SearchService.Options
import org.readium.r2.shared.util.Ref
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.text.StringCharacterIterator

/**
 * Base implementation of [SearchService] iterating through the content of Publication's
 * resources.
 *
 * To stay media-type-agnostic, [StringSearchService] relies on [ResourceContentExtractor]
 * implementations to retrieve the pure text content from markups (e.g. HTML) or binary (e.g. PDF)
 * resources.
 *
 * Subclasses must implement the actual text search algorithm.
 */
abstract class StringSearchService(
    val publication: Ref<Publication>,
    val snippetLength: Int,
    val extractorFactory: ResourceContentExtractor.Factory,
) : SearchService {

    companion object {
        fun createDefaultFactory(
            snippetLength: Int = 200,
            extractorFactory: ResourceContentExtractor.Factory = DefaultResourceContentExtractorFactory()
        ): (Publication.Service.Context) -> StringSearchService =
            { context ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    IcuSearchService(context.publication, snippetLength, extractorFactory)
                } else {
                    NaiveSearchService(context.publication, snippetLength, extractorFactory)
                }
            }
    }

    /**
     * Find occurrence ranges of the provided [query] in a resource's [text] content.
     */
    protected abstract fun findRanges(text: String, query: String, options: Options): List<IntRange>

    override suspend fun search(query: String, options: Options?): SearchTry<SearchIterator> =
        try {
            val publication = publication() ?: throw IllegalStateException("No Publication object")
            Try.success(Iterator(publication, query, options ?: Options()))

        } catch (e: Exception) {
            Try.failure(SearchException.wrap(e))
        }

    private inner class Iterator(val publication: Publication, val query: String, val options: Options) : SearchIterator {
        /**
         * Index of the last reading order resource searched in.
         */
        private var index = -1

        override suspend fun next(): SearchTry<LocatorCollection?> {
            try {
                if (index >= publication.readingOrder.count() - 1) {
                    return Try.success(null)
                }

                index += 1

                val link = publication.readingOrder[index]
                val resource = publication.get(link)

                val text = extractorFactory.createExtractor(resource)?.extractText(resource)?.getOrThrow()
                if (text == null) {
                    Timber.w("Cannot extract text from resource: ${link.href}")
                    return next()
                }

                val locators = findLocators(index, link, text)
                // If no occurrences were found in the current resource, skip to the next one
                // automatically.
                if (locators.isEmpty()) {
                    return next()
                }

                return Try.success(LocatorCollection(locators = locators))

            } catch (e: Exception) {
                return Try.failure(SearchException.wrap(e))
            }
        }

        private suspend fun findLocators(resourceIndex: Int, link: Link, text: String): List<Locator> {
            if (text == "")
                return emptyList()

            val resourceTitle = publication.tableOfContents.titleMatching(link.href)
            val resourceLocator = link.toLocator().copy(
                title = resourceTitle ?: link.title
            )
            val locators = mutableListOf<Locator>()

            withContext(Dispatchers.IO) {
                for (range in findRanges(text = text, query = query, options)) {
                    locators.add(createLocator(resourceIndex, resourceLocator, text, range))
                }
            }

            return locators
        }

        private suspend fun createLocator(resourceIndex: Int, resourceLocator: Locator, text: String, range: IntRange): Locator {
            val progression = range.first.toDouble() / text.length.toDouble()

            var totalProgression: Double? = null
            val positions = positions()
            val resourceStartTotalProg = positions.getOrNull(resourceIndex)?.firstOrNull()?.locations?.totalProgression
            if (resourceStartTotalProg != null) {
                val resourceEndTotalProg = positions.getOrNull(resourceIndex + 1)?.firstOrNull()?.locations?.totalProgression ?: 1.0
                totalProgression = resourceStartTotalProg + progression * (resourceEndTotalProg - resourceStartTotalProg)
            }

            return resourceLocator.copy(
                locations = resourceLocator.locations.copy(
                    progression = progression,
                    totalProgression = totalProgression,
                ),
                text = createSnippet(text, range),
            )
        }

        /**
         * Extracts a snippet from the given [text] at the provided highlight [range].
         *
         * Makes sure that words are not cut off at the boundaries.
         */
        private fun createSnippet(text: String, range: IntRange): Locator.Text {
            val iter = StringCharacterIterator(text)

            var before = ""
            iter.index = range.first
            var char = iter.previous()
            var count = snippetLength
            while (char != StringCharacterIterator.DONE && (count >= 0 || !char.isWhitespace())) {
                before = char + before
                count--
                char = iter.previous()
            }

            var after = ""
            iter.index = range.last
            char = iter.next()
            count = snippetLength
            while (char != StringCharacterIterator.DONE && (count >= 0 || !char.isWhitespace())) {
                after += char
                count--
                char = iter.next()
            }

            return Locator.Text(
                highlight = text.substring(range),
                before = before,
                after = after,
            )
        }

        private lateinit var _positions: List<List<Locator>>
        private suspend fun positions(): List<List<Locator>> {
            if (!::_positions.isInitialized) {
                _positions = publication.positionsByReadingOrder()
            }
            return _positions
        }
    }
}

private fun List<Link>.titleMatching(href: String): String? {
    for (link in this) {
        link.titleMatching(href)?.let { return it }
    }
    return null
}

private fun Link.titleMatching(targetHref: String): String? {
    if (href.substringBeforeLast("#") == targetHref) {
        return title
    }
    return children.titleMatching(targetHref)
}
