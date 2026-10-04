package com.example.ytdlp.download

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ytdlp.ytdlp.DownloadRequest
import java.io.File

data class ResolvedDownloadedMedia(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentUri: String?,
    val filePath: String?
)

object DownloadedMediaResolver {

    fun resolve(
        context: Context,
        request: DownloadRequest,
        successMessage: String
    ): ResolvedDownloadedMedia? {

        val fileName =
            extractFileName(
                successMessage
            )
                ?: return null

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            resolveFromMediaStore(
                context = context,
                fileName = fileName
            )

        } else {

            resolveLegacy(
                context = context,
                fileName = fileName
            )
        }
    }

    private fun extractFileName(
        successMessage: String
    ): String? {

        val marker =
            "File:\n"

        val markerIndex =
            successMessage.lastIndexOf(
                marker
            )

        if (markerIndex < 0) {
            return null
        }

        return successMessage
            .substring(
                markerIndex +
                        marker.length
            )
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun resolveFromMediaStore(
        context: Context,
        fileName: String
    ): ResolvedDownloadedMedia? {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {
            return null
        }

        val resolver =
            context.contentResolver

        val collection =
            MediaStore.Downloads
                .EXTERNAL_CONTENT_URI

        val projection =
            arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_ADDED
            )

        val selection =
            "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                    "${MediaStore.Downloads.RELATIVE_PATH} = ?"

        val selectionArgs =
            arrayOf(
                fileName,
                Environment.DIRECTORY_DOWNLOADS +
                        "/SyDown/"
            )

        val sortOrder =
            "${MediaStore.Downloads.DATE_ADDED} DESC"

        return try {

            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                if (!cursor.moveToFirst()) {
                    return@use null
                }

                val idIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Downloads._ID
                    )

                val nameIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Downloads.DISPLAY_NAME
                    )

                val mimeIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Downloads.MIME_TYPE
                    )

                val sizeIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Downloads.SIZE
                    )

                val id =
                    cursor.getLong(
                        idIndex
                    )

                val actualName =
                    cursor
                        .getString(
                            nameIndex
                        )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: fileName

                val mimeType =
                    cursor
                        .getString(
                            mimeIndex
                        )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: mimeTypeFromName(
                            actualName
                        )

                val size =
                    cursor
                        .getLong(
                            sizeIndex
                        )
                        .coerceAtLeast(
                            0L
                        )

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                ResolvedDownloadedMedia(
                    fileName =
                        actualName,

                    mimeType =
                        mimeType,

                    sizeBytes =
                        size,

                    contentUri =
                        uri.toString(),

                    filePath =
                        null
                )
            }

        } catch (
            _: Throwable
        ) {

            null
        }
    }

    private fun resolveLegacy(
        context: Context,
        fileName: String
    ): ResolvedDownloadedMedia? {

        val directory =
            context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS
            )
                ?: context.filesDir

        val file =
            File(
                directory,
                fileName
            )

        if (
            !file.exists() ||
            !file.isFile
        ) {
            return null
        }

        return ResolvedDownloadedMedia(
            fileName =
                file.name,

            mimeType =
                mimeTypeFromName(
                    file.name
                ),

            sizeBytes =
                file.length(),

            contentUri =
                null,

            filePath =
                file.absolutePath
        )
    }

    private fun mimeTypeFromName(
        fileName: String
    ): String {

        val extension =
            fileName
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()

        return when (
            extension
        ) {

            "mp4" ->
                "video/mp4"

            "m4a" ->
                "audio/mp4"

            "webm" ->
                "video/webm"

            "mp3" ->
                "audio/mpeg"

            "opus" ->
                "audio/opus"

            "ogg",
            "oga" ->
                "audio/ogg"

            "aac" ->
                "audio/aac"

            "wav" ->
                "audio/wav"

            "flac" ->
                "audio/flac"

            "mkv" ->
                "video/x-matroska"

            "mov" ->
                "video/quicktime"

            else ->
                "application/octet-stream"
        }
    }
}