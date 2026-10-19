package com.example.ytdlp.ytdlp

import android.content.Context
import com.example.ytdlp.BuildConfig
import com.example.ytdlp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object BundledYtDlpManager {

    suspend fun prepare(context: Context) {
        withContext(Dispatchers.IO) {

            if (BuildConfig.IS_OWNER_BUILD) {
                return@withContext
            }

            val appContext =
                context.applicationContext

            val installedFile =
                File(
                    appContext.noBackupFilesDir,
                    "youtubedl-android/yt-dlp/yt-dlp"
                )

            val parent =
                installedFile.parentFile
                    ?: error("Invalid yt-dlp directory")

            val temporaryFile =
                File(parent, "yt-dlp.sydown-new")

            val backupFile =
                File(parent, "yt-dlp.sydown-backup")

            /*
             * Restore an interrupted replacement first.
             */
            if (
                !installedFile.exists() &&
                backupFile.exists()
            ) {
                check(
                    backupFile.renameTo(installedFile)
                ) {
                    "Could not restore previous yt-dlp"
                }
            }

            /*
             * On clean installs, YoutubeDL.init()
             * will extract the bundled file itself.
             */
            if (!installedFile.exists()) {
                return@withContext
            }

            val bundledHash =
                appContext.resources
                    .openRawResource(R.raw.ytdlp)
                    .use(::sha256)

            val installedHash =
                installedFile
                    .inputStream()
                    .use(::sha256)

            if (
                bundledHash.contentEquals(installedHash)
            ) {
                return@withContext
            }

            /*
             * Stage and verify the bundled executable.
             */
            if (temporaryFile.exists()) {
                check(
                    temporaryFile.delete()
                ) {
                    "Could not remove temporary yt-dlp"
                }
            }

            try {
                appContext.resources
                    .openRawResource(R.raw.ytdlp)
                    .use { input ->
                        temporaryFile
                            .outputStream()
                            .use { output ->
                                input.copyTo(output)
                            }
                    }

                check(
                    temporaryFile.length() > 0L
                ) {
                    "Bundled yt-dlp is empty"
                }

                check(
                    temporaryFile.inputStream()
                        .use(::sha256)
                        .contentEquals(bundledHash)
                ) {
                    "Bundled yt-dlp verification failed"
                }

                check(
                    temporaryFile.setExecutable(
                        true,
                        true
                    )
                ) {
                    "Could not make yt-dlp executable"
                }

                /*
                 * Keep the previous executable as backup.
                 */
                if (backupFile.exists()) {
                    check(
                        backupFile.delete()
                    ) {
                        "Could not remove old backup"
                    }
                }

                check(
                    installedFile.renameTo(backupFile)
                ) {
                    "Could not back up previous yt-dlp"
                }

                try {
                    check(
                        temporaryFile.renameTo(installedFile)
                    ) {
                        "Could not install bundled yt-dlp"
                    }

                    backupFile.delete()

                } catch (error: Throwable) {

                    if (!installedFile.exists()) {
                        backupFile.renameTo(installedFile)
                    }

                    throw error
                }

            } finally {
                temporaryFile.delete()
            }
        }
    }

    private fun sha256(input: InputStream): ByteArray {

        val digest =
            MessageDigest.getInstance("SHA-256")

        val buffer =
            ByteArray(8192)

        while (true) {

            val count =
                input.read(buffer)

            if (count < 0) {
                break
            }

            if (count > 0) {
                digest.update(
                    buffer,
                    0,
                    count
                )
            }
        }

        return digest.digest()
    }
}