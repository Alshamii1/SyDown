package com.example.ytdlp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ytdlp.ui.theme.YTDLPTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        val sharedUrl =
            intent
                ?.getStringExtra(
                    LauncherActivity.EXTRA_SHARED_URL
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val appPreferences =
            AppPreferences(
                applicationContext
            )

        setContent {

            var languagePreference by remember {
                mutableStateOf(
                    appPreferences.getLanguage()
                )
            }

            var themeMode by remember {
                mutableStateOf(
                    appPreferences.getThemeMode()
                )
            }

            val resolvedLanguage =
                when (languagePreference) {

                    AppLanguage.SYSTEM ->
                        SyDownLocale.resolveDeviceLanguage()

                    else ->
                        languagePreference
                }

            val useDarkTheme =
                when (themeMode) {

                    AppThemeMode.SYSTEM ->
                        isSystemInDarkTheme()

                    AppThemeMode.DARK ->
                        true

                    AppThemeMode.LIGHT ->
                        false
                }

            val localizedContext =
                SyDownLocale.localizedContext(
                    context =
                        LocalContext.current,

                    language =
                        resolvedLanguage
                )

            val layoutDirection =
                when (resolvedLanguage) {

                    AppLanguage.ENGLISH ->
                        LayoutDirection.Ltr

                    else ->
                        LayoutDirection.Rtl
                }

            CompositionLocalProvider(
                LocalContext provides
                        localizedContext,

                LocalLayoutDirection provides
                        layoutDirection
            ) {

                YTDLPTheme(
                    darkTheme =
                        useDarkTheme
                ) {

                    SyDownApp(
                        languagePreference =
                            languagePreference,

                        themeMode =
                            themeMode,

                        sharedUrl =
                            sharedUrl,

                        onLanguageChanged = {
                                language ->

                            appPreferences
                                .setLanguage(
                                    language
                                )

                            languagePreference =
                                language
                        },

                        onThemeChanged = {
                                newTheme ->

                            appPreferences
                                .setThemeMode(
                                    newTheme
                                )

                            themeMode =
                                newTheme
                        }
                    )
                }
            }
        }
    }
}