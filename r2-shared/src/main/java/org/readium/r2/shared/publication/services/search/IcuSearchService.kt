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
import org.readium.r2.shared.publication.services.positionsByReadingOrder
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
            Try.success(Iterator(publication, query, options))

        } catch (e: Exception) {
            Try.failure(SearchException.wrap(e))
        }

    private inner class Iterator(val publication: Publication, val query: String, val options: Set<Option>) : SearchIterator {
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                return emptyList() // FIXME
            if (text == "")
                return emptyList()

            val locators = mutableListOf<Locator>()

            withContext(Dispatchers.IO) {
                val iter = createStringSearch(text)
                var start = iter.first()
                while (start != android.icu.text.SearchIterator.DONE) {
                    val range = start until (start + iter.matchLength)
                    locators.add(createLocator(resourceIndex, link.toLocator(), text, range))

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
            val locale = publication.metadata.locale ?: Locale.getDefault()
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
                text = Locator.Text(
                    highlight = text.substring(range),
                    before = text.substring((range.first - snippetLength).coerceAtLeast(0) until range.first),
                    after = text.substring((range.last + 1) until (range.last + snippetLength).coerceAtMost(text.length)),
                )
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

    companion object {
        fun createFactory(
            snippetLength: Int = 200,
            extractorFactory: ResourceContentExtractor.Factory = DefaultResourceContentExtractorFactory()
        ): (Publication.Service.Context) -> IcuSearchService =
            { context -> IcuSearchService(context.publication, snippetLength, extractorFactory) }
    }
}
