package com.example.ytdlp

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object SyDownLocale {

    fun localizedContext(
        context: Context,
        language: AppLanguage
    ): Context {

        val resolvedLanguage =
            when (language) {

                AppLanguage.ARABIC ->
                    AppLanguage.ARABIC

                AppLanguage.ENGLISH ->
                    AppLanguage.ENGLISH

                AppLanguage.SYSTEM ->
                    resolveDeviceLanguage()
            }

        val locale =
            Locale.forLanguageTag(
                resolvedLanguage.code
                    ?: "ar"
            )

        val configuration =
            Configuration(
                context.resources.configuration
            )

        configuration.setLocale(
            locale
        )

        configuration.setLayoutDirection(
            locale
        )

        return context.createConfigurationContext(
            configuration
        )
    }

    fun resolveDeviceLanguage(): AppLanguage {

        val deviceLanguage =
            Locale
                .getDefault()
                .language
                .lowercase(
                    Locale.ROOT
                )

        return when (deviceLanguage) {

            "ar" ->
                AppLanguage.ARABIC

            "en" ->
                AppLanguage.ENGLISH

            else ->
                AppLanguage.ARABIC
        }
    }
}