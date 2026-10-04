package com.example.ytdlp.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object DownloadThumbnailStore {

    private const val DIRECTORY_NAME =
        "download_history_thumbnails"

    private const val CONNECT_TIMEOUT_MS =
        10_000

    private const val READ_TIMEOUT_MS =
        15_000

    private const val MAX_IMAGE_BYTES =
        10L * 1024L * 1024L

    /*
     * يحفظ Thumbnail في التخزين الداخلي الدائم للتطبيق.
     *
     * عند النجاح يعيد المسار المحلي الكامل.
     * عند عدم وجود رابط أو فشل الشبكة/الصورة يعيد null.
     *
     * فشل Thumbnail لا يجب أبدًا أن يفشل تنزيل الوسائط.
     */
    suspend fun save(
        context: Context,
        thumbnailUrl: String?,
        historyId: String
    ): String? {
        val cleanUrl =
            thumbnailUrl
                ?.trim()
                ?.takeIf {
                    it.startsWith(
                        "http://",
                        ignoreCase = true
                    ) ||
                            it.startsWith(
                                "https://",
                                ignoreCase = true
                            )
                }
                ?: return null

        if (historyId.isBlank()) {
            return null
        }

        return withContext(
            Dispatchers.IO
        ) {
            try {
                saveInternal(
                    context =
                        context.applicationContext,
                    thumbnailUrl =
                        cleanUrl,
                    historyId =
                        historyId
                )
            } catch (_: Throwable) {
                null
            }
        }
    }

    /*
     * يحذف Thumbnail المرتبط بمعرف History.
     *
     * سنستخدمه عندما يفشل التنزيل أو يتم إلغاؤه،
     * حتى لا تبقى صور يتيمة داخل التخزين الداخلي.
     */
    suspend fun delete(
        context: Context,
        historyId: String
    ) {
        if (historyId.isBlank()) {
            return
        }

        withContext(
            Dispatchers.IO
        ) {
            try {
                val destination =
                    getDestinationFile(
                        context =
                            context.applicationContext,
                        historyId =
                            historyId
                    )

                if (
                    destination.exists()
                ) {
                    destination.delete()
                }

                val tempFile =
                    getTemporaryFile(
                        context =
                            context.applicationContext,
                        historyId =
                            historyId
                    )

                if (
                    tempFile.exists()
                ) {
                    tempFile.delete()
                }
            } catch (_: Throwable) {
                // التنظيف الإضافي لا يجب أن يسبب خطأ للتطبيق.
            }
        }
    }

    private fun saveInternal(
        context: Context,
        thumbnailUrl: String,
        historyId: String
    ): String? {
        val directory =
            getDirectory(
                context
            )

        if (
            !directory.exists() &&
            !directory.mkdirs()
        ) {
            return null
        }

        val destination =
            getDestinationFile(
                context = context,
                historyId = historyId
            )

        /*
         * إن كانت الصورة موجودة مسبقًا وصالحة،
         * لا نعيد تنزيلها.
         */
        if (
            destination.exists() &&
            destination.isFile &&
            destination.length() > 0L
        ) {
            return destination.absolutePath
        }

        val tempFile =
            getTemporaryFile(
                context = context,
                historyId = historyId
            )

        /*
         * تنظيف أي ملف مؤقت تركته محاولة قديمة.
         */
        if (
            tempFile.exists()
        ) {
            tempFile.delete()
        }

        val connection =
            (
                    URL(
                        thumbnailUrl
                    )
                        .openConnection()
                            as? HttpURLConnection
                    )
                ?: return null

        try {
            connection.instanceFollowRedirects =
                true

            connection.connectTimeout =
                CONNECT_TIMEOUT_MS

            connection.readTimeout =
                READ_TIMEOUT_MS

            connection.useCaches =
                true

            connection.setRequestProperty(
                "Accept",
                "image/*"
            )

            connection.connect()

            val responseCode =
                connection.responseCode

            if (
                responseCode !in
                200..299
            ) {
                return null
            }

            val contentLength =
                connection.contentLengthLong

            if (
                contentLength >
                MAX_IMAGE_BYTES
            ) {
                return null
            }

            val bytes =
                connection
                    .inputStream
                    .use { input ->
                        readLimitedBytes(
                            input = input,
                            maxBytes =
                                MAX_IMAGE_BYTES
                        )
                    }
                    ?: return null

            if (bytes.isEmpty()) {
                return null
            }

            val bitmap =
                BitmapFactory
                    .decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )
                    ?: return null

            val thumbnail =
                scaleDown(
                    bitmap = bitmap,
                    maxWidth = 720,
                    maxHeight = 720
                )

            try {
                val written =
                    writeBitmap(
                        bitmap = thumbnail,
                        destination = tempFile
                    )

                if (!written) {
                    return null
                }

                if (
                    destination.exists()
                ) {
                    destination.delete()
                }

                val moved =
                    tempFile.renameTo(
                        destination
                    )

                if (!moved) {
                    val copied =
                        copyFile(
                            source = tempFile,
                            destination =
                                destination
                        )

                    if (!copied) {
                        destination.delete()
                        return null
                    }

                    tempFile.delete()
                }

                if (
                    !destination.exists() ||
                    !destination.isFile ||
                    destination.length() <= 0L
                ) {
                    destination.delete()
                    return null
                }

                return destination.absolutePath

            } finally {
                if (
                    thumbnail !== bitmap
                ) {
                    try {
                        thumbnail.recycle()
                    } catch (_: Throwable) {
                        // لا شيء
                    }
                }

                try {
                    bitmap.recycle()
                } catch (_: Throwable) {
                    // لا شيء
                }

                if (
                    tempFile.exists()
                ) {
                    tempFile.delete()
                }
            }

        } finally {
            connection.disconnect()
        }
    }

    private fun writeBitmap(
        bitmap: Bitmap,
        destination: File
    ): Boolean {
        return try {
            destination
                .parentFile
                ?.mkdirs()

            FileOutputStream(
                destination
            ).use { output ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    86,
                    output
                )
            }
        } catch (_: Throwable) {
            try {
                destination.delete()
            } catch (_: Throwable) {
                // لا شيء
            }

            false
        }
    }

    private fun copyFile(
        source: File,
        destination: File
    ): Boolean {
        return try {
            source
                .inputStream()
                .use { input ->

                    FileOutputStream(
                        destination
                    ).use { output ->

                        input.copyTo(
                            output
                        )
                    }
                }

            destination.exists() &&
                    destination.length() > 0L

        } catch (_: Throwable) {
            try {
                destination.delete()
            } catch (_: Throwable) {
                // لا شيء
            }

            false
        }
    }

    private fun readLimitedBytes(
        input: InputStream,
        maxBytes: Long
    ): ByteArray? {
        val output =
            ByteArrayOutputStream()

        val buffer =
            ByteArray(
                8 * 1024
            )

        var total =
            0L

        while (true) {
            val count =
                input.read(
                    buffer
                )

            if (count < 0) {
                break
            }

            if (count == 0) {
                continue
            }

            total +=
                count.toLong()

            if (
                total >
                maxBytes
            ) {
                return null
            }

            output.write(
                buffer,
                0,
                count
            )
        }

        return output.toByteArray()
    }

    private fun scaleDown(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        if (
            bitmap.width <= maxWidth &&
            bitmap.height <= maxHeight
        ) {
            return bitmap
        }

        val widthRatio =
            maxWidth.toFloat() /
                    bitmap.width.toFloat()

        val heightRatio =
            maxHeight.toFloat() /
                    bitmap.height.toFloat()

        val ratio =
            minOf(
                widthRatio,
                heightRatio
            )

        val targetWidth =
            (bitmap.width * ratio)
                .toInt()
                .coerceAtLeast(1)

        val targetHeight =
            (bitmap.height * ratio)
                .toInt()
                .coerceAtLeast(1)

        return Bitmap
            .createScaledBitmap(
                bitmap,
                targetWidth,
                targetHeight,
                true
            )
    }

    private fun getDirectory(
        context: Context
    ): File {
        return File(
            context.filesDir,
            DIRECTORY_NAME
        )
    }

    private fun getDestinationFile(
        context: Context,
        historyId: String
    ): File {
        val safeId =
            sha256(
                historyId
            )

        return File(
            getDirectory(
                context
            ),
            "$safeId.jpg"
        )
    }

    private fun getTemporaryFile(
        context: Context,
        historyId: String
    ): File {
        val safeId =
            sha256(
                historyId
            )

        return File(
            getDirectory(
                context
            ),
            "$safeId.tmp"
        )
    }

    private fun sha256(
        value: String
    ): String {
        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )
                .digest(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return digest.joinToString(
            separator = ""
        ) {
            "%02x".format(it)
        }
    }
}