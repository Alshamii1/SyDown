package com.example.ytdlp.owner

import android.content.Context
import com.example.ytdlp.ytdlp.YtDlpManager

object OwnerYtDlpTools {

    suspend fun getVersion(
        context: Context
    ): String {
        val appContext =
            context.applicationContext

        YtDlpManager.initialize(
            appContext
        )

        return YtDlpManager
            .getYtDlpVersion()
            .trim()
    }

    suspend fun update(
        context: Context
    ): OwnerYtDlpUpdateResult {
        val appContext =
            context.applicationContext

        YtDlpManager.initialize(
            appContext
        )

        val previousVersion =
            YtDlpManager
                .getYtDlpVersion()
                .trim()

        val rawResult =
            YtDlpManager.updateYtDlp(
                appContext
            )

        val currentVersion =
            YtDlpManager
                .getYtDlpVersion()
                .trim()

        return OwnerYtDlpUpdateResult(
            previousVersion =
                previousVersion,
            currentVersion =
                currentVersion,
            changed =
                previousVersion !=
                        currentVersion,
            rawResult =
                rawResult
        )
    }
}

data class OwnerYtDlpUpdateResult(
    val previousVersion: String,
    val currentVersion: String,
    val changed: Boolean,
    val rawResult: String
)