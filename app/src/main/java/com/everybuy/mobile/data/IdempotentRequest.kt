package com.everybuy.mobile.data

import kotlinx.coroutines.CancellationException
import java.io.IOException

internal suspend fun <T> withIdempotentReplay(request: suspend () -> T): T {
    try {
        return request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        return request()
    } catch (e: retrofit2.HttpException) {
        if (e.code() in 500..599) return request()
        throw e
    }
}
