package com.example.ytdlp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val percent: Int?
        get() {
            if (totalBytes <= 0L) {
                return null
            }

            return (
                    (downloadedBytes * 100L) /
                            totalBytes
                    )
                .coerceIn(0L, 100L)
                .toInt()
        }
}

object AppUpdateInstaller {

    private const val UPDATE_DIRECTORY =
        "app_updates"

    private const val UPDATE_FILE_NAME =
        "SyDown-update.apk"

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (
            AppUpdateDownloadProgress
        ) -> Unit
    ): File =
        withContext(Dispatchers.IO) {
            require(
                apkUrl.startsWith("https://")
            ) {
                "Invalid APK URL"
            }

            val updateDirectory =
                File(
                    context.filesDir,
                    UPDATE_DIRECTORY
                ).apply {
                    if (!exists()) {
                        mkdirs()
                    }
                }

            val finalFile =
                File(
                    updateDirectory,
                    UPDATE_FILE_NAME
                )

            val temporaryFile =
                File(
                    updateDirectory,
                    "$UPDATE_FILE_NAME.part"
                )

            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }

            val connection =
                URL(apkUrl)
                    .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                connection.useCaches = false
                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.android.package-archive,application/octet-stream,*/*"
                )

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !in 200..299
                ) {
                    throw IllegalStateException(
                        "APK download failed: HTTP $responseCode"
                    )
                }

                val totalBytes =
                    connection.contentLengthLong

                var downloadedBytes = 0L

                connection.inputStream.use { input ->
                    temporaryFile
                        .outputStream()
                        .buffered()
                        .use { output ->
                            val buffer =
                                ByteArray(
                                    DEFAULT_BUFFER_SIZE
                                )

                            while (true) {
                                val read =
                                    input.read(buffer)

                                if (read < 0) {
                                    break
                                }

                                output.write(
                                    buffer,
                                    0,
                                    read
                                )

                                downloadedBytes +=
                                    read.toLong()

                                onProgress(
                                    AppUpdateDownloadProgress(
                                        downloadedBytes =
                                            downloadedBytes,
                                        totalBytes =
                                            totalBytes
                                    )
                                )
                            }

                            output.flush()
                        }
                }

                require(
                    temporaryFile.length() > 0L
                ) {
                    "Downloaded APK is empty"
                }

                if (finalFile.exists()) {
                    finalFile.delete()
                }

                require(
                    temporaryFile.renameTo(
                        finalFile
                    )
                ) {
                    "Could not prepare downloaded APK"
                }

                finalFile
            } catch (
                throwable: Throwable
            ) {
                temporaryFile.delete()
                throw throwable
            } finally {
                connection.disconnect()
            }
        }

    fun canRequestPackageInstalls(
        context: Context
    ): Boolean {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            context.packageManager
                .canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownAppsSettings(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val intent =
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse(
                    "package:${context.packageName}"
                )
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        context.startActivity(intent)
    }

    fun installApk(
        context: Context,
        apkFile: File
    ) {
        require(
            apkFile.exists() &&
                    apkFile.length() > 0L
        ) {
            "APK file does not exist"
        }

        val apkUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {
                setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(intent)
    }
}