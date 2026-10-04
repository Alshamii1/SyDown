package com.example.ytdlp

enum class SyDownErrorType {
    RATE_LIMITED,
    ACCESS_FORBIDDEN,
    NETWORK,
    TIMEOUT,
    UNSUPPORTED_URL,
    LOGIN_REQUIRED,
    MEDIA_UNAVAILABLE,
    GENERIC
}

object SyDownErrorClassifier {

    fun classify(
        error: Throwable
    ): SyDownErrorType {

        val raw =
            buildString {

                error.message
                    ?.let {
                        append(it)
                        append('\n')
                    }

                append(
                    error.toString()
                )
            }

        return classify(raw)
    }

    fun classify(
        rawError: String
    ): SyDownErrorType {

        val lower =
            rawError.lowercase()

        return when {

            containsAny(
                lower,
                "429",
                "too many requests",
                "confirm you're not a bot",
                "rate limit",
                "rate-limit",
                "rate limited"
            ) ->
                SyDownErrorType.RATE_LIMITED

            containsAny(
                lower,
                "403",
                "forbidden",
                "access denied"
            ) ->
                SyDownErrorType.ACCESS_FORBIDDEN

            containsAny(
                lower,
                "unable to resolve host",
                "name or service not known",
                "temporary failure in name resolution",
                "network is unreachable",
                "no route to host",
                "connection refused",
                "connection reset",
                "network error"
            ) ->
                SyDownErrorType.NETWORK

            containsAny(
                lower,
                "timed out",
                "timeout",
                "read timeout",
                "connect timeout"
            ) ->
                SyDownErrorType.TIMEOUT

            containsAny(
                lower,
                "login required",
                "sign in",
                "sign-in",
                "log in",
                "login to",
                "authentication required",
                "cookies are required",
                "requires authentication",
                "private video",
                "private account"
            ) ->
                SyDownErrorType.LOGIN_REQUIRED

            containsAny(
                lower,
                "unsupported url",
                "unsupported site",
                "no suitable extractor",
                "invalid url",
                "is not a valid url"
            ) ->
                SyDownErrorType.UNSUPPORTED_URL

            containsAny(
                lower,
                "video unavailable",
                "media unavailable",
                "content unavailable",
                "this video is unavailable",
                "not available",
                "has been removed",
                "deleted video",
                "does not exist"
            ) ->
                SyDownErrorType.MEDIA_UNAVAILABLE

            else ->
                SyDownErrorType.GENERIC
        }
    }

    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean {

        return values.any {
                value ->

            text.contains(
                value
            )
        }
    }
}