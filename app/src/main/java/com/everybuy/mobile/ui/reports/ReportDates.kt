package com.everybuy.mobile.ui.reports

import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthPattern = Regex("""^(?!0000)\d{4}-(0[1-9]|1[0-2])$""")
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("en", "NG"))

fun isReportMonth(value: String): Boolean = monthPattern.matches(value.trim())

fun currentLagosMonth(): String = YearMonth.now(ZoneId.of("Africa/Lagos")).toString()

fun shiftMonth(month: String, delta: Long): String =
    runCatching { YearMonth.parse(month).plusMonths(delta).toString() }.getOrDefault(month)

fun formatMonthLabel(month: String): String =
    runCatching { YearMonth.parse(month).format(monthLabelFormatter) }.getOrDefault(month)
