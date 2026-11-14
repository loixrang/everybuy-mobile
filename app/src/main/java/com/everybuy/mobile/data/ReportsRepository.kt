package com.everybuy.mobile.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton class ReportsRepository @Inject constructor(private val api: ReportsApi) {
    suspend fun dailyOverview(): DailyOverview = api.dailyOverview()
}
