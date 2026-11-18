package com.everybuy.mobile.core.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val nairaFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
    currency = Currency.getInstance("NGN")
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

fun formatNaira(amount: String): String = runCatching {
    synchronized(nairaFormatter) { nairaFormatter.format(BigDecimal(amount)) }
}.getOrDefault("₦$amount")

fun formatLagosDateTime(isoDateTime: String): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale("en", "NG")).withZone(ZoneId.of("Africa/Lagos")).format(Instant.parse(isoDateTime))
}.getOrDefault(isoDateTime)

fun formatLagosDate(isoDateTime: String): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("en", "NG")).withZone(ZoneId.of("Africa/Lagos")).format(Instant.parse(isoDateTime))
}.getOrDefault(isoDateTime)
