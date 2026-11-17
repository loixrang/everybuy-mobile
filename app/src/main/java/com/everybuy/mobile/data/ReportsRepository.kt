package com.everybuy.mobile.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton class ReportsRepository @Inject constructor(private val api: ReportsApi) {
    suspend fun dailyOverview(): DailyOverview = api.dailyOverview()
    suspend fun monthly(month: String): MonthlyReportDto = api.monthly(month)
    suspend fun profit(month: String): ProfitReportDto = api.profit(month)
}
