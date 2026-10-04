package com.example.ytdlp

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
fun SyDownSettingsDialog(
    onDismissRequest: () -> Unit,
    layoutDirection: LayoutDirection,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
    ) {
        val dialogComposeView =
            LocalView.current

        val dialogWindowProvider =
            dialogComposeView.parent as? DialogWindowProvider

        val dialogWindow =
            dialogWindowProvider?.window

        val androidLayoutDirection =
            when (layoutDirection) {
                LayoutDirection.Rtl ->
                    View.LAYOUT_DIRECTION_RTL

                LayoutDirection.Ltr ->
                    View.LAYOUT_DIRECTION_LTR
            }

        var directionReady by remember(
            dialogWindow,
            layoutDirection
        ) {
            mutableStateOf(false)
        }

        DisposableEffect(
            dialogWindow,
            dialogComposeView,
            layoutDirection
        ) {
            val decorView =
                dialogWindow?.decorView

            if (decorView != null) {
                /*
                 * نثبت الاتجاه على نافذة Android الخاصة بالـ Dialog
                 * وعلى ComposeView الموجود بداخلها قبل إظهار البطاقة.
                 */
                decorView.layoutDirection =
                    androidLayoutDirection

                dialogComposeView.layoutDirection =
                    androidLayoutDirection

                decorView.requestLayout()
                dialogComposeView.requestLayout()

                /*
                 * لا نظهر محتوى SyDown حتى تصبح النافذة نفسها
                 * بالاتجاه الصحيح.
                 */
                directionReady =
                    true
            }

            onDispose {
                directionReady =
                    false
            }
        }

        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {
                if (directionReady) {
                    content()
                }
            }
        }
    }
}