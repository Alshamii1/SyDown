package com.example.ytdlp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * =====================================================
 * SYDOWN DARK COLOR SCHEME
 * =====================================================
 */

private val SyDownDarkColorScheme =
    darkColorScheme(

        primary =
            SyGreenLight,

        onPrimary =
            SyBlack,

        primaryContainer =
            SyGreenDark,

        onPrimaryContainer =
            SyWhite,

        secondary =
            SyWhite,

        onSecondary =
            SyBlack,

        secondaryContainer =
            SyDarkSurfaceVariant,

        onSecondaryContainer =
            SyDarkTextPrimary,

        tertiary =
            SyRedLight,

        onTertiary =
            SyWhite,

        tertiaryContainer =
            SyRedDark,

        onTertiaryContainer =
            SyWhite,

        error =
            SyRedLight,

        onError =
            SyWhite,

        errorContainer =
            SyRedDark,

        onErrorContainer =
            SyWhite,

        background =
            SyDarkBackground,

        onBackground =
            SyDarkTextPrimary,

        surface =
            SyDarkSurface,

        onSurface =
            SyDarkTextPrimary,

        surfaceVariant =
            SyDarkSurfaceVariant,

        onSurfaceVariant =
            SyDarkTextSecondary,

        outline =
            SyDarkBorder,

        outlineVariant =
            SyDarkSurfaceElevated
    )

/*
 * =====================================================
 * SYDOWN LIGHT COLOR SCHEME
 * =====================================================
 */

private val SyDownLightColorScheme =
    lightColorScheme(

        primary =
            SyGreenDark,

        onPrimary =
            SyWhite,

        primaryContainer =
            SyGreen,

        onPrimaryContainer =
            SyWhite,

        secondary =
            SyBlack,

        onSecondary =
            SyWhite,

        secondaryContainer =
            SyLightSurfaceVariant,

        onSecondaryContainer =
            SyLightTextPrimary,

        tertiary =
            SyRed,

        onTertiary =
            SyWhite,

        tertiaryContainer =
            SyRedLight,

        onTertiaryContainer =
            SyWhite,

        error =
            SyRed,

        onError =
            SyWhite,

        errorContainer =
            SyRedLight,

        onErrorContainer =
            SyWhite,

        background =
            SyLightBackground,

        onBackground =
            SyLightTextPrimary,

        surface =
            SyLightSurface,

        onSurface =
            SyLightTextPrimary,

        surfaceVariant =
            SyLightSurfaceVariant,

        onSurfaceVariant =
            SyLightTextSecondary,

        outline =
            SyLightBorder,

        outlineVariant =
            SyLightSurfaceVariant
    )

/*
 * =====================================================
 * SYDOWN THEME
 * =====================================================
 *
 * darkTheme يبقى parameter واضحًا حتى نستطيع لاحقًا
 * ربطه بإعداد المستخدم:
 *
 * - Dark
 * - Light
 * - System
 *
 * Dynamic Colors غير مستخدمة عمدًا حتى تبقى
 * هوية SyDown ثابتة على جميع الأجهزة.
 */

@Composable
fun YTDLPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {

            SyDownDarkColorScheme

        } else {

            SyDownLightColorScheme
        }

    /*
     * نضبط Status Bar و Navigation Bar أيضًا
     * لكي تكون هوية التطبيق متكاملة.
     */
    val view =
        LocalView.current

    if (!view.isInEditMode) {

        SideEffect {

            val window =
                (view.context as? Activity)
                    ?.window
                    ?: return@SideEffect

            window.statusBarColor =
                colorScheme.background
                    .toArgb()

            window.navigationBarColor =
                colorScheme.background
                    .toArgb()

            WindowCompat
                .getInsetsController(
                    window,
                    view
                )
                .apply {

                    isAppearanceLightStatusBars =
                        !darkTheme

                    isAppearanceLightNavigationBars =
                        !darkTheme
                }
        }
    }

    MaterialTheme(
        colorScheme =
            colorScheme,

        typography =
            Typography,

        content =
            content
    )
}