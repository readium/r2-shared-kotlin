/*
 * Copyight 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import org.readium.r2.shared.fetcher.ResourceContentExtractor
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService.Option
import org.readium.r2.shared.util.Ref

/**
 * A naive [SearchService] performing exact matches on strings.
 *
 * There are no safe ways to perform case insensitive search using String.indexOf() with
 * all languages, so this [SearchService] does not have any options. Use [IcuSearchService] for
 * better results.
 */
class NaiveSearchService(
    publication: Ref<Publication>,
    snippetLength: Int,
    extractorFactory: ResourceContentExtractor.Factory,
) : StringSearchService(publication, snippetLength, extractorFactory) {

    /**
     */
    override val options: Set<Option> get() = setOf()

    override fun findRanges(text: String, query: String, options: Set<Option>): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var index: Int = text.indexOf(query)
        while (index >= 0) {
            ranges.add(index until (index + query.length))
            index = text.indexOf(query, index + 1)
        }
        return ranges
    }
}
