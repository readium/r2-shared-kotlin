/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import android.icu.text.StringSearch
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.fetcher.DefaultResourceContentExtractorFactory
import org.readium.r2.shared.fetcher.ResourceContentExtractor
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Ref
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.text.StringCharacterIterator
import java.util.*

/**
 * A basic implementation of [SearchService] iterating through the content of Publication's
 * resources and using ICU components to perform the actual search.
 *
 * To stay media-type-agnostic, [IcuSearchService] relies on [ResourceContentExtractor]
 * implementations to retrieve the pure text content from markups (e.g. HTML) or binary (e.g. PDF)
 * resources.
 */
class IcuSearchService private constructor(
    private val publication: Ref<Publication>,
    private val surroundingContextLength: Int,
    private val extractorFactory: ResourceContentExtractor.Factory,
) : SearchService {

    override val capabilities: Set<SearchService.Capability> get() = emptySet()
    override val supportedOptions: Set<SearchService.Option> get() = emptySet()
    override val defaultOptions: Set<SearchService.Option> get() = emptySet()

    override suspend fun search(query: String, options: Set<SearchService.Option>): SearchTry<SearchIterator> =
        try {
            val publication = publication() ?: throw IllegalStateException("No Publication object")
            val locale = publication.metadata.locale ?: Locale.getDefault()
            Try.success(Iterator(locale, query, options))

        } catch (e: Exception) {
            Try.failure(SearchException.wrap(e))
        }

    private inner class Iterator(val locale: Locale, val query: String, val options: Set<SearchService.Option>) : SearchIterator {
        /**
         * Index of the last reading order resource searched in.
         */
        private var index = -1

        override suspend fun next(): SearchTry<LocatorCollection?> {
            try {
                val publication = publication()
                if (publication == null || index >= publication.readingOrder.count() - 1) {
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

                val locators = findLocators(link, text)
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

        private suspend fun findLocators(link: Link, text: String): List<Locator> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                return emptyList() // FIXME
            if (text == "")
                return emptyList()

            val locators = mutableListOf<Locator>()

            withContext(Dispatchers.IO) {
                val collator = Collator.getInstance(locale) as RuleBasedCollator
                // http://userguide.icu-project.org/collation/customization
                // PRIMARY = ignore accents and case
                // PRIMARY + caseLevel true = ignore accents
                // SECONDARY = ignore case
                collator.strength = Collator.SECONDARY
//            collator.isCaseLevel = true
                val iter = StringSearch(query, StringCharacterIterator(text), collator)
                var start = iter.first()
                while (start != android.icu.text.SearchIterator.DONE) {
                    val end = start + iter.matchLength

                    var locator = link.toLocator()
                    locator = locator.copy(
                        locations = locator.locations.copy(
                            progression = start.toDouble() / text.length.toDouble(),
                        ),
                        text = Locator.Text(
                            highlight = text.substring(start until end),
                            before = text.substring((start - surroundingContextLength).coerceAtLeast(0) until start),
                            after = text.substring(end until (end + surroundingContextLength).coerceAtMost(text.length)),
                        )
                    )
                    locators.add(locator)

                    start = iter.next()
                }
            }

            return locators
        }
   }

    companion object {
        fun createFactory(
            surroundingContextLength: Int = 70,
            extractorFactory: ResourceContentExtractor.Factory = DefaultResourceContentExtractorFactory()
        ): (Publication.Service.Context) -> IcuSearchService =
            { context -> IcuSearchService(context.publication, surroundingContextLength, extractorFactory) }
    }
}
