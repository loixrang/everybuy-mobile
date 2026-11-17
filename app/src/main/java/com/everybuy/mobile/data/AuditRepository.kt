package com.everybuy.mobile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class AuditEntryDto(
    val actor: String = "Unknown",
    val action: String = "",
    val entity: String = "",
    val entityId: String = "",
    val at: String = "",
    val detail: JsonObject? = null,
)

interface AuditApi {
    @retrofit2.http.GET("api/v1/audit") suspend fun auditLog(): List<AuditEntryDto>
}

@Singleton class AuditRepository @Inject constructor(private val api: AuditApi) {
    suspend fun list(): List<AuditEntryDto> = api.auditLog()
}
