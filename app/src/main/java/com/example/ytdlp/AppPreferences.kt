package com.example.ytdlp

import android.content.Context
import java.util.Locale

enum class AppLanguage(
    val code: String?
) {
    SYSTEM(null),
    ARABIC("ar"),
    ENGLISH("en")
}

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

class AppPreferences(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun isOnboardingCompleted(): Boolean {
        return preferences.getBoolean(
            KEY_ONBOARDING_COMPLETED,
            false
        )
    }

    fun setOnboardingCompleted(
        completed: Boolean
    ) {
        preferences
            .edit()
            .putBoolean(
                KEY_ONBOARDING_COMPLETED,
                completed
            )
            .apply()
    }

    /*
     * اللغة المحفوظة التي اختارها المستخدم.
     *
     * SYSTEM = اتبع لغة الجهاز.
     * ARABIC = العربية دائمًا.
     * ENGLISH = الإنجليزية دائمًا.
     */
    fun getLanguage(): AppLanguage {

        val saved =
            preferences.getString(
                KEY_LANGUAGE,
                AppLanguage.SYSTEM.name
            )

        return runCatching {
            AppLanguage.valueOf(
                saved ?: AppLanguage.SYSTEM.name
            )
        }.getOrDefault(
            AppLanguage.SYSTEM
        )
    }

    fun setLanguage(
        language: AppLanguage
    ) {
        preferences
            .edit()
            .putString(
                KEY_LANGUAGE,
                language.name
            )
            .apply()
    }

    /*
     * تعيد اللغة التي يجب عرض الواجهة بها فعليًا.
     *
     * إذا كان الاختيار SYSTEM:
     * العربية إذا كان الجهاز عربيًا.
     * الإنجليزية إذا كان الجهاز إنجليزيًا.
     * العربية كـ fallback لأي لغة أخرى.
     */
    fun getResolvedLanguage(): AppLanguage {

        val selected =
            getLanguage()

        if (selected != AppLanguage.SYSTEM) {
            return selected
        }

        return detectDeviceLanguage()
    }

    /*
     * تستخدمها شاشة Onboarding لتحديد اللغة الأولية
     * اعتمادًا على لغة الجهاز.
     */
    fun getInitialOnboardingLanguage(): AppLanguage {

        return if (hasExplicitLanguageChoice()) {
            getResolvedLanguage()
        } else {
            detectDeviceLanguage()
        }
    }

    fun hasExplicitLanguageChoice(): Boolean {

        return preferences.contains(
            KEY_LANGUAGE
        ) &&
                getLanguage() !=
                AppLanguage.SYSTEM
    }

    fun getThemeMode(): AppThemeMode {

        val saved =
            preferences.getString(
                KEY_THEME,
                AppThemeMode.SYSTEM.name
            )

        return runCatching {
            AppThemeMode.valueOf(
                saved ?: AppThemeMode.SYSTEM.name
            )
        }.getOrDefault(
            AppThemeMode.SYSTEM
        )
    }

    fun setThemeMode(
        themeMode: AppThemeMode
    ) {
        preferences
            .edit()
            .putString(
                KEY_THEME,
                themeMode.name
            )
            .apply()
    }

    private fun detectDeviceLanguage(): AppLanguage {

        val languageCode =
            Locale
                .getDefault()
                .language
                .lowercase(
                    Locale.ROOT
                )

        return when (languageCode) {

            "ar" ->
                AppLanguage.ARABIC

            "en" ->
                AppLanguage.ENGLISH

            else ->
                AppLanguage.ARABIC
        }
    }

    companion object {

        private const val PREFS_NAME =
            "sydown_preferences"

        private const val KEY_ONBOARDING_COMPLETED =
            "onboarding_completed"

        private const val KEY_LANGUAGE =
            "language"

        private const val KEY_THEME =
            "theme"
    }
}