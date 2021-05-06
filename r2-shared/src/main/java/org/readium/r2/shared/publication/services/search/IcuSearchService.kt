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
import org.readium.r2.shared.fetcher.ResourceContentExtractor
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService.Option
import org.readium.r2.shared.util.Ref
import java.text.StringCharacterIterator
import java.util.*

/**
 * Implementation of [SearchService] using ICU components to perform the actual search while taking
 * into account languages specificities.
 */
@RequiresApi(Build.VERSION_CODES.N)
class IcuSearchService(
    publication: Ref<Publication>,
    snippetLength: Int,
    extractorFactory: ResourceContentExtractor.Factory,
) : StringSearchService(publication, snippetLength, extractorFactory) {

    override val options: Set<Option> get() = setOf(
        Option.CaseSensitive(false),
        Option.DiacriticSensitive(false),
        Option.WholeWord(false),
    )

    override fun findRanges(text: String, query: String, options: Set<Option>): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val iter = createStringSearch(text, query)
        var start = iter.first()
        while (start != android.icu.text.SearchIterator.DONE) {
            ranges.add(start until (start + iter.matchLength))
            start = iter.next()
        }
        return ranges
    }

    private val locale: Locale by lazy { publication()?.metadata?.locale ?: Locale.getDefault() }

    private fun createStringSearch(text: String, query: String): StringSearch {
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
