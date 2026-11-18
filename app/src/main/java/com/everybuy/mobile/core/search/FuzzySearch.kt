package com.everybuy.mobile.core.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

private const val FUZZY_SEARCH_DEBOUNCE_MS = 150L

fun <T> fuzzySearch(
    items: List<T>,
    query: String,
    searchableText: (T) -> String,
): List<T> {
    val queryWords = query.toSearchWords()
    if (queryWords.isEmpty()) return items

    return items.mapIndexedNotNull { index, item ->
        val itemWords = searchableText(item).toSearchWords()
        var score = 0
        for (queryWord in queryWords) {
            val quality = itemWords.minOfOrNull { itemWord -> matchQuality(queryWord, itemWord) ?: Int.MAX_VALUE }
            if (quality == null || quality == Int.MAX_VALUE) return@mapIndexedNotNull null
            score += quality
        }
        RankedItem(index, score, item)
    }.sortedWith(compareBy<RankedItem<T>> { it.score }.thenBy { it.index }).map { it.value }
}

class DebouncedFuzzySearch<T>(
    private val scope: CoroutineScope,
) {
    private var searchJob: Job? = null

    fun search(
        items: List<T>,
        query: String,
        searchableText: (T) -> String,
        onResults: (List<T>) -> Unit,
    ) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchJob = null
            onResults(items)
            return
        }
        searchJob = scope.launch {
            delay(FUZZY_SEARCH_DEBOUNCE_MS)
            val results = withContext(Dispatchers.Default) {
                fuzzySearch(items, query, searchableText)
            }
            onResults(results)
        }
    }

    fun cancel() {
        searchJob?.cancel()
        searchJob = null
    }
}

private data class RankedItem<T>(val index: Int, val score: Int, val value: T)

private fun String.toSearchWords(): List<String> {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        .lowercase(Locale.ROOT)
        .filterNot {
            it.category == CharCategory.NON_SPACING_MARK ||
                it.category == CharCategory.COMBINING_SPACING_MARK ||
                it.category == CharCategory.ENCLOSING_MARK
        }
    val words = mutableListOf<String>()
    val word = StringBuilder()
    var previousWasDigit: Boolean? = null

    fun flushWord() {
        if (word.isNotEmpty()) words += word.toString()
        word.clear()
        previousWasDigit = null
    }

    for (character in normalized) {
        if (!character.isLetterOrDigit()) {
            flushWord()
            continue
        }
        val isDigit = character.isDigit()
        if (previousWasDigit != null && previousWasDigit != isDigit) flushWord()
        word.append(character)
        previousWasDigit = isDigit
    }
    flushWord()
    return words
}

private fun matchQuality(queryWord: String, itemWord: String): Int? {
    val queryIsNumeric = queryWord.all(Char::isDigit)
    val itemIsNumeric = itemWord.all(Char::isDigit)
    if (queryIsNumeric || itemIsNumeric) return if (queryWord == itemWord) 0 else null

    if (queryWord == itemWord) return 0
    if (itemWord.startsWith(queryWord)) return 1
    if (queryWord.length >= 3 && itemWord.contains(queryWord)) return 2

    if (queryWord.length >= 4) {
        val matchingPrefix = (queryWord.length - 1..queryWord.length + 1)
            .filter { it >= 4 && it <= itemWord.length }
            .any { prefixLength ->
                editDistanceAtMost(queryWord, itemWord.take(prefixLength), 1) <= 1
            }
        if (matchingPrefix) return 3
    }
    if (queryWord.length >= 5 && itemWord.length >= 5 && editDistanceAtMost(queryWord, itemWord, 2) <= 2) {
        return 3
    }
    return null
}

private fun editDistanceAtMost(left: String, right: String, maxDistance: Int): Int {
    if (kotlin.math.abs(left.length - right.length) > maxDistance) return maxDistance + 1
    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)
    for (leftIndex in left.indices) {
        current[0] = leftIndex + 1
        var rowMinimum = current[0]
        for (rightIndex in right.indices) {
            val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
            current[rightIndex + 1] = minOf(
                previous[rightIndex + 1] + 1,
                current[rightIndex] + 1,
                previous[rightIndex] + substitutionCost,
            )
            rowMinimum = minOf(rowMinimum, current[rightIndex + 1])
        }
        if (rowMinimum > maxDistance) return maxDistance + 1
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.length]
}
