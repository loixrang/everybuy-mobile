package com.everybuy.mobile.core.util

import java.text.Normalizer
import java.util.Locale

fun matchesSearch(name: String, query: String): Boolean {
    fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

    val words = normalize(query).split(Regex("\\s+")).filter(String::isNotEmpty)
    if (words.isEmpty()) return true
    val target = normalize(name)
    return words.all(target::contains)
}
