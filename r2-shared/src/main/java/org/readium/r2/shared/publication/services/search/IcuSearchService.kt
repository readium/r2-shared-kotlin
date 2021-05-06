/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.icu.text.BreakIterator
import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import android.icu.text.StringSearch
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.fetcher.DefaultResourceContentExtractorFactory
import org.readium.r2.shared.fetcher.ResourceContentExtractor
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.search.SearchService.Option
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
    private val snippetLength: Int,
    private val extractorFactory: ResourceContentExtractor.Factory,
) : SearchService {

    override val options: Set<Option> get() = setOf(
        Option.CaseSensitive(false),
        Option.DiacriticSensitive(false),
        Option.WholeWord(false),
    )

    override suspend fun search(query: String, options: Set<Option>): SearchTry<SearchIterator> =
        try {
            val publication = publication() ?: throw IllegalStateException("No Publication object")
            val locale = publication.metadata.locale ?: Locale.getDefault()
            Try.success(Iterator(locale, query, options))

        } catch (e: Exception) {
            Try.failure(SearchException.wrap(e))
        }

    private inner class Iterator(val locale: Locale, val query: String, val options: Set<Option>) : SearchIterator {
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
                val iter = createStringSearch(text)
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
                            before = text.substring((start - snippetLength).coerceAtLeast(0) until start),
                            after = text.substring(end until (end + snippetLength).coerceAtMost(text.length)),
                        )
                    )
                    locators.add(locator)

                    start = iter.next()
                }
            }

            return locators
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun createStringSearch(text: String): StringSearch {
            val caseSensitive = options.get<Option.CaseSensitive>()?.on ?: false
            var diacriticSensitive = options.get<Option.DiacriticSensitive>()?.on ?: false
            val wholeWord = options.get<Option.WholeWord>()?.on ?: false

            // Because of an issue (see FIXME below), we can't have case sensitivity without also
            // enabling diacritic sensitivity.
            diacriticSensitive = diacriticSensitive || caseSensitive

            // http://userguide.icu-project.org/collation/customization
            // ignore diacritics and case = primary strength
            // ignore diacritics = primary strength + caseLevel on
            // ignore case = secondary strength
            val collator = Collator.getInstance(locale) as RuleBasedCollator
            if (!diacriticSensitive) {
                collator.strength = Collator.PRIMARY
                if (caseSensitive) {
                    // FIXME: This doesn't seem to work despite the documentation indicating:
                    // > To ignore accents but take cases into account, set strength to primary and case level to on.
                    // > http://userguide.icu-project.org/collation/customization
                    collator.isCaseLevel = true
                }
            } else if (!caseSensitive) {
                collator.strength = Collator.SECONDARY
            }

            val breakIterator: BreakIterator? =
                if (wholeWord) BreakIterator.getWordInstance()
                else null

            return StringSearch(query, StringCharacterIterator(text), collator, breakIterator)
        }
    }

    companion object {
        fun createFactory(
            snippetLength: Int = 200,
            extractorFactory: ResourceContentExtractor.Factory = DefaultResourceContentExtractorFactory()
        ): (Publication.Service.Context) -> IcuSearchService =
            { context -> IcuSearchService(context.publication, snippetLength, extractorFactory) }
    }
}
