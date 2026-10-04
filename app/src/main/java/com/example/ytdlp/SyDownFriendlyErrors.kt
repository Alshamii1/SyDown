package com.example.ytdlp

import android.content.Context

object SyDownFriendlyErrors {

    fun message(
        context: Context,
        error: Throwable
    ): String {

        return message(
            context = context,
            type =
                SyDownErrorClassifier
                    .classify(error)
        )
    }

    fun message(
        context: Context,
        rawError: String
    ): String {

        return message(
            context = context,
            type =
                SyDownErrorClassifier
                    .classify(rawError)
        )
    }

    private fun message(
        context: Context,
        type: SyDownErrorType
    ): String {

        val resource =
            when (type) {

                SyDownErrorType.RATE_LIMITED ->
                    R.string.error_rate_limited

                SyDownErrorType.ACCESS_FORBIDDEN ->
                    R.string.error_access_forbidden

                SyDownErrorType.NETWORK ->
                    R.string.error_network

                SyDownErrorType.TIMEOUT ->
                    R.string.error_timeout

                SyDownErrorType.UNSUPPORTED_URL ->
                    R.string.error_unsupported_url

                SyDownErrorType.LOGIN_REQUIRED ->
                    R.string.error_login_required

                SyDownErrorType.MEDIA_UNAVAILABLE ->
                    R.string.error_media_unavailable

                SyDownErrorType.GENERIC ->
                    R.string.error_generic
            }

        return context.getString(
            resource
        )
    }
}