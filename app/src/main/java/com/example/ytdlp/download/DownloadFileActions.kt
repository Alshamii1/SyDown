package com.example.ytdlp.download

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object DownloadFileActions {

    /*
     * =====================================================
     * OPEN
     * =====================================================
     */

    fun open(
        context: Context,
        item: DownloadHistoryItem
    ): Boolean {

        val uri =
            resolveUri(
                context = context,
                item = item
            )
                ?: return false

        if (
            !exists(
                context = context,
                item = item
            )
        ) {
            return false
        }

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                setDataAndType(
                    uri,
                    item.mimeType
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: "*/*"
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                if (
                    context !is android.app.Activity
                ) {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
            }

        return try {

            context.startActivity(
                intent
            )

            true

        } catch (
            _: ActivityNotFoundException
        ) {

            false

        } catch (
            _: Throwable
        ) {

            false
        }
    }

    /*
     * =====================================================
     * SHARE
     * =====================================================
     */

    fun share(
        context: Context,
        item: DownloadHistoryItem
    ): Boolean {

        val uri =
            resolveUri(
                context = context,
                item = item
            )
                ?: return false

        if (
            !exists(
                context = context,
                item = item
            )
        ) {
            return false
        }

        val sendIntent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    item.mimeType
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: "*/*"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                clipData =
                    android.content.ClipData.newRawUri(
                        item.fileName,
                        uri
                    )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        val chooser =
            Intent.createChooser(
                sendIntent,
                null
            )

        if (
            context !is android.app.Activity
        ) {
            chooser.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

        return try {

            context.startActivity(
                chooser
            )

            true

        } catch (
            _: Throwable
        ) {

            false
        }
    }

    /*
     * =====================================================
     * EXISTS
     * =====================================================
     */

    fun exists(
        context: Context,
        item: DownloadHistoryItem
    ): Boolean {

        val storedContentUri =
            item.contentUri
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            storedContentUri != null
        ) {

            val uri =
                try {

                    Uri.parse(
                        storedContentUri
                    )

                } catch (
                    _: Throwable
                ) {

                    return false
                }

            return try {

                context
                    .contentResolver
                    .openFileDescriptor(
                        uri,
                        "r"
                    )
                    ?.use {
                        true
                    }
                    ?: false

            } catch (
                _: Throwable
            ) {

                false
            }
        }

        val path =
            item.filePath
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return false

        val file =
            File(path)

        return file.exists() &&
                file.isFile
    }

    /*
     * =====================================================
     * DELETE FROM DEVICE
     * =====================================================
     */

    fun deleteFromDevice(
        context: Context,
        item: DownloadHistoryItem
    ): Boolean {

        val storedContentUri =
            item.contentUri
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            storedContentUri != null
        ) {

            val uri =
                try {

                    Uri.parse(
                        storedContentUri
                    )

                } catch (
                    _: Throwable
                ) {

                    return false
                }

            return try {

                val deletedRows =
                    context
                        .contentResolver
                        .delete(
                            uri,
                            null,
                            null
                        )

                deletedRows > 0 ||
                        !exists(
                            context = context,
                            item = item
                        )

            } catch (
                _: SecurityException
            ) {

                false

            } catch (
                _: Throwable
            ) {

                false
            }
        }

        val path =
            item.filePath
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return false

        val file =
            File(path)

        if (
            !file.exists()
        ) {
            return true
        }

        if (
            !file.isFile
        ) {
            return false
        }

        return try {

            file.delete() ||
                    !file.exists()

        } catch (
            _: Throwable
        ) {

            false
        }
    }

    /*
     * =====================================================
     * RESOLVE URI
     * =====================================================
     */

    fun resolveUri(
        context: Context,
        item: DownloadHistoryItem
    ): Uri? {

        val storedContentUri =
            item.contentUri
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            storedContentUri != null
        ) {

            return try {

                Uri.parse(
                    storedContentUri
                )

            } catch (
                _: Throwable
            ) {

                null
            }
        }

        val path =
            item.filePath
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null

        val file =
            File(path)

        if (
            !file.exists() ||
            !file.isFile
        ) {
            return null
        }

        return try {

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        } catch (
            _: Throwable
        ) {

            null
        }
    }
}