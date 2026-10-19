package com.example.ytdlp.ytdlp

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Manages media analysis requests.
 *
 * Features:
 * - Short-lived successful result cache.
 * - Deduplication of simultaneous requests.
 * - Temporary cooldown after clear rate-limit errors.
 * - No automatic retries.
 * - Safe cleanup when an analysis is cancelled.
 */
internal object AnalysisRequestManager {

    private const val CACHE_DURATION_MS = 60_000L

    private const val MAX_CACHE_ENTRIES = 10

    private const val RATE_LIMIT_COOLDOWN_MS = 120_000L

    private data class CacheEntry(
        val json: String,
        val createdAt: Long
    )

    private val mutex = Mutex()

    private val cache =
        LinkedHashMap<String, CacheEntry>()

    private val activeRequests =
        mutableMapOf<String, CompletableDeferred<String>>()

    private val restrictedUntil =
        mutableMapOf<String, Long>()

    suspend fun analyze(
        url: String,
        executeAnalysis: suspend () -> String
    ): String {
        val normalizedUrl = url.trim()

        require(normalizedUrl.isNotBlank()) {
            "URL cannot be empty"
        }

        coroutineContext.ensureActive()

        val platform = platformKey(normalizedUrl)

        var cachedResult: String? = null

        var existingRequest: CompletableDeferred<String>? = null

        var ownedRequest: CompletableDeferred<String>? = null

        mutex.withLock {
            val now = SystemClock.elapsedRealtime()

            removeExpiredEntries(now)

            val restrictionEnd =
                restrictedUntil[platform] ?: 0L

            if (now < restrictionEnd) {
                throw IllegalStateException(
                    "Request temporarily restricted. " +
                            "Please wait before trying again."
                )
            }

            val cached = cache[normalizedUrl]

            if (cached != null) {
                cachedResult = cached.json
            } else {
                val active = activeRequests[normalizedUrl]

                if (active != null) {
                    existingRequest = active
                } else {
                    val deferred = CompletableDeferred<String>()

                    activeRequests[normalizedUrl] = deferred

                    ownedRequest = deferred
                }
            }
        }

        cachedResult?.let {
            return it
        }

        existingRequest?.let {
            return it.await()
        }

        val deferred = requireNotNull(ownedRequest)

        try {
            coroutineContext.ensureActive()

            val result = executeAnalysis()

            coroutineContext.ensureActive()

            if (result.isBlank()) {
                throw IllegalStateException(
                    "Media analysis returned an empty result"
                )
            }

            mutex.withLock {
                coroutineContext.ensureActive()

                cache[normalizedUrl] = CacheEntry(
                    json = result,
                    createdAt = SystemClock.elapsedRealtime()
                )

                trimCache()

                if (
                    activeRequests[normalizedUrl] === deferred
                ) {
                    activeRequests.remove(normalizedUrl)
                }

                deferred.complete(result)
            }

            return result
        } catch (error: Throwable) {

            /*
             * Cancellation must not be interpreted
             * as a network failure or rate limit.
             *
             * NonCancellable guarantees cleanup even
             * when the owning coroutine was cancelled.
             */
            withContext(NonCancellable) {
                mutex.withLock {

                    if (
                        error !is CancellationException &&
                        isRateLimitError(error)
                    ) {
                        restrictedUntil[platform] =
                            SystemClock.elapsedRealtime() +
                                    RATE_LIMIT_COOLDOWN_MS
                    }

                    if (
                        activeRequests[normalizedUrl] === deferred
                    ) {
                        activeRequests.remove(normalizedUrl)
                    }

                    if (error is CancellationException) {
                        deferred.cancel(
                            CancellationException(
                                "Media analysis was cancelled"
                            )
                        )
                    } else {
                        deferred.completeExceptionally(error)
                    }
                }
            }

            throw error
        }
    }

    private fun removeExpiredEntries(now: Long) {
        val cacheIterator = cache.entries.iterator()

        while (cacheIterator.hasNext()) {
            val entry = cacheIterator.next()

            if (
                now - entry.value.createdAt >=
                CACHE_DURATION_MS
            ) {
                cacheIterator.remove()
            }
        }

        val restrictionIterator =
            restrictedUntil.entries.iterator()

        while (restrictionIterator.hasNext()) {
            val entry = restrictionIterator.next()

            if (now >= entry.value) {
                restrictionIterator.remove()
            }
        }
    }

    private fun trimCache() {
        while (cache.size > MAX_CACHE_ENTRIES) {
            val oldestKey =
                cache.keys.firstOrNull() ?: break

            cache.remove(oldestKey)
        }
    }

    private fun isRateLimitError(
        error: Throwable
    ): Boolean {
        val message = error.message
            .orEmpty()
            .lowercase(Locale.ROOT)

        return message.contains("http error 429") ||
                message.contains("http 429") ||
                message.contains("error 429") ||
                message.contains("too many requests") ||
                message.contains("rate limit exceeded") ||
                message.contains("temporarily blocked") ||
                message.contains("rate-limited")
    }

    private fun platformKey(
        url: String
    ): String {
        return try {
            val host = URI(url)
                .host
                ?.lowercase(Locale.ROOT)
                .orEmpty()

            if (host.isNotBlank()) {
                host
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }
}