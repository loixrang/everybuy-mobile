package com.everybuy.mobile.core.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Reads the human-readable message from a Nest/Zod error response without
 * exposing transport or response details to the UI. Malformed bodies and
 * absent messages return null so callers can use their own fallback.
 */
fun HttpException.serverMessage(): String? = runCatching<String?> {
    val body = response()?.errorBody()?.string().orEmpty()
    if (body.isBlank()) {
        null
    } else {
        val payload = errorJson.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
        when (val message = payload["message"]) {
            is JsonPrimitive -> message.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonArray -> message.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .takeIf { it.isNotBlank() }
            else -> null
        }
    }
}.getOrNull()
