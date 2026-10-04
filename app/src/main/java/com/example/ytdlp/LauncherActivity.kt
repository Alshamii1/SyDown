package com.example.ytdlp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ytdlp.ui.theme.YTDLPTheme
import kotlinx.coroutines.delay

class LauncherActivity : ComponentActivity() {

    private var pendingSharedUrl: String? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        pendingSharedUrl =
            extractSharedUrl(
                intent
            )

        val appPreferences =
            AppPreferences(
                applicationContext
            )

        setContent {
            YTDLPTheme {
                LauncherContent(
                    appPreferences =
                        appPreferences
                )
            }
        }
    }

    @Composable
    private fun LauncherContent(
        appPreferences: AppPreferences
    ) {
        var splashFinished by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(Unit) {
            delay(800L)

            splashFinished =
                true
        }

        if (!splashFinished) {
            SyDownSplashScreen()
            return
        }

        if (
            appPreferences
                .isOnboardingCompleted()
        ) {
            LaunchedEffect(Unit) {
                openMainActivity(
                    sharedUrl =
                        pendingSharedUrl
                )
            }

            SyDownSplashScreen()
        } else {
            OnboardingContent(
                appPreferences =
                    appPreferences
            )
        }
    }

    @Composable
    private fun OnboardingContent(
        appPreferences: AppPreferences
    ) {
        var selectedLanguage by remember {
            mutableStateOf(
                appPreferences
                    .getInitialOnboardingLanguage()
            )
        }

        val localizedContext =
            SyDownLocale.localizedContext(
                context =
                    LocalContext.current,
                language =
                    selectedLanguage
            )

        val layoutDirection =
            when (selectedLanguage) {

                AppLanguage.ENGLISH ->
                    LayoutDirection.Ltr

                AppLanguage.ARABIC ->
                    LayoutDirection.Rtl

                AppLanguage.SYSTEM -> {
                    when (
                        SyDownLocale
                            .resolveDeviceLanguage()
                    ) {
                        AppLanguage.ENGLISH ->
                            LayoutDirection.Ltr

                        else ->
                            LayoutDirection.Rtl
                    }
                }
            }

        CompositionLocalProvider(
            LocalContext provides
                    localizedContext,
            LocalLayoutDirection provides
                    layoutDirection
        ) {
            OnboardingScreen(
                selectedLanguage =
                    selectedLanguage,

                onLanguageSelected = {
                        language ->

                    selectedLanguage =
                        language
                },

                onGetStarted = {
                    appPreferences
                        .setLanguage(
                            selectedLanguage
                        )

                    appPreferences
                        .setOnboardingCompleted(
                            true
                        )

                    openMainActivity(
                        sharedUrl =
                            pendingSharedUrl
                    )
                }
            )
        }
    }

    private fun extractSharedUrl(
        sourceIntent: Intent?
    ): String? {

        if (
            sourceIntent?.action !=
            Intent.ACTION_SEND
        ) {
            return null
        }

        val sharedText =
            sourceIntent
                .getStringExtra(
                    Intent.EXTRA_TEXT
                )
                ?.trim()
                .orEmpty()

        if (
            sharedText.isBlank()
        ) {
            return null
        }

        val urlRegex =
            Regex(
                pattern =
                    """https?://[^\s]+""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val rawUrl =
            urlRegex
                .find(
                    sharedText
                )
                ?.value
                ?: return null

        return rawUrl
            .trimEnd(
                '.',
                ',',
                ';',
                ':',
                '!',
                '?',
                ')',
                ']',
                '}',
                '،',
                '؛'
            )
            .takeIf {
                it.isNotBlank()
            }
    }

    private fun openMainActivity(
        sharedUrl: String? = null
    ) {
        val mainIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                sharedUrl
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        putExtra(
                            EXTRA_SHARED_URL,
                            it
                        )
                    }
            }

        startActivity(
            mainIntent
        )

        finish()
    }

    companion object {

        const val EXTRA_SHARED_URL =
            "com.example.ytdlp.extra.SHARED_URL"
    }
}